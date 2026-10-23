package zw.co.tncybertech.dispute.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.dispute.api.dto.TrainingDtos;
import zw.co.tncybertech.dispute.domain.training.*;
import zw.co.tncybertech.dispute.repo.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

  private final RbzPolicyRepository policyRepo;
  private final RbzQuizRepository quizRepo;
  private final RbzQuizQuestionRepository questionRepo;
  private final RbzQuizOptionRepository optionRepo;
  private final TrainingAttemptRepository attemptRepo;
  private final TrainingAnswerRepository answerRepo;

  @Transactional(readOnly = true)
  public List<TrainingDtos.PolicySummary> listPolicies() {
    return policyRepo.findByActiveTrue().stream()
        .map(p -> new TrainingDtos.PolicySummary(
            p.getId(),
            p.getTitle(),
            p.getCircularReference(),
            p.getSummary(),
            p.getEffectiveDate(),
            p.getKeyRequirements()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TrainingDtos.QuizDetail> listQuizzes(Long policyId) {
    List<RbzQuiz> quizzes = policyId == null ? quizRepo.findAll() : quizRepo.findByPolicyId(policyId);
    return quizzes.stream().map(this::toQuizDetail).toList();
  }

  @Transactional(readOnly = true)
  public TrainingDtos.QuizDetail getQuiz(Long quizId) {
    RbzQuiz quiz = quizRepo.findById(quizId)
        .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
    return toQuizDetail(quiz);
  }

  @Transactional
  public TrainingDtos.AttemptResult submitAttempt(Long userId, String role, TrainingDtos.SubmitAttemptRequest req) {
    RbzQuiz quiz = quizRepo.findById(req.quizId())
        .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

    List<RbzQuizQuestion> questions = questionRepo.findByQuizId(quiz.getId());
    Map<Long, RbzQuizQuestion> questionById = questions.stream()
        .collect(Collectors.toMap(RbzQuizQuestion::getId, q -> q));

    Map<Long, RbzQuizOption> correctOptions = new HashMap<>();
    for (RbzQuizQuestion question : questions) {
      List<RbzQuizOption> options = optionRepo.findByQuestionId(question.getId());
      RbzQuizOption correct = options.stream().filter(RbzQuizOption::isCorrect).findFirst()
          .orElseThrow(() -> new IllegalStateException("Question missing a correct option"));
      correctOptions.put(question.getId(), correct);
    }

    List<TrainingDtos.Answer> answers = req.answers() == null ? List.of() : req.answers();
    int score = 0;
    int maxScore = questions.size();

    TrainingAttempt attempt = TrainingAttempt.builder()
        .userId(userId)
        .userRole(role)
        .quiz(quiz)
        .score(0)
        .maxScore(maxScore)
        .build();

    attempt = attemptRepo.save(attempt);
    List<TrainingDtos.Weakness> weaknesses = new java.util.ArrayList<>();

    for (TrainingDtos.Answer answer : answers) {
      RbzQuizQuestion question = questionById.get(answer.questionId());
      if (question == null) {
        continue;
      }
      RbzQuizOption option = optionRepo.findById(answer.optionId())
          .orElseThrow(() -> new IllegalArgumentException("Invalid option"));
      if (!option.getQuestion().getId().equals(question.getId())) {
        throw new IllegalArgumentException("Option does not belong to question");
      }
      boolean correct = option.isCorrect();
      if (correct) {
        score++;
      } else {
        weaknesses.add(new TrainingDtos.Weakness(question.getId(), question.getQuestion(), question.getExplanation()));
      }
      answerRepo.save(TrainingAnswer.builder()
          .attempt(attempt)
          .question(question)
          .selectedOptionId(option.getId())
          .correct(correct)
          .build());
    }

    attempt.setScore(score);
    attemptRepo.save(attempt);

    return new TrainingDtos.AttemptResult(score, maxScore, weaknesses);
  }

  @Transactional(readOnly = true)
  public TrainingDtos.AwarenessMetrics awarenessMetrics(Instant from, Instant to) {
    List<TrainingAttempt> attempts = attemptRepo.findByCreatedAtBetween(from, to);
    double average = attempts.stream()
        .mapToDouble(a -> scorePercent(a.getScore(), a.getMaxScore()))
        .average()
        .orElse(0);

    Map<Long, PolicyAggregate> policyMap = new HashMap<>();
    Map<String, RoleAggregate> roleMap = new HashMap<>();
    for (TrainingAttempt attempt : attempts) {
      Long policyId = attempt.getQuiz().getPolicy().getId();
      policyMap.computeIfAbsent(policyId, k -> new PolicyAggregate(attempt.getQuiz().getPolicy().getTitle()))
          .accept(attempt);

      String role = attempt.getUserRole();
      roleMap.computeIfAbsent(role, k -> new RoleAggregate(role))
          .accept(attempt);
    }

    List<TrainingDtos.PolicyAwareness> byPolicy = policyMap.entrySet().stream()
        .map(entry -> new TrainingDtos.PolicyAwareness(
            entry.getKey(),
            entry.getValue().policyTitle,
            entry.getValue().average(),
            entry.getValue().count
        ))
        .toList();

    List<TrainingDtos.RoleAwareness> byRole = roleMap.values().stream()
        .map(entry -> new TrainingDtos.RoleAwareness(
            entry.role,
            entry.average(),
            entry.count
        ))
        .toList();

    return new TrainingDtos.AwarenessMetrics(average, attempts.size(), byPolicy, byRole);
  }

  @Transactional(readOnly = true)
  public List<TrainingDtos.AttemptSummary> myAttempts(Long userId) {
    return attemptRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(a -> new TrainingDtos.AttemptSummary(a.getId(), a.getQuiz().getTitle(), a.getScore(), a.getMaxScore(), a.getCreatedAt()))
        .toList();
  }

  private TrainingDtos.QuizDetail toQuizDetail(RbzQuiz quiz) {
    List<RbzQuizQuestion> questions = questionRepo.findByQuizId(quiz.getId());
    List<TrainingDtos.QuestionDto> questionDtos = questions.stream()
        .map(q -> new TrainingDtos.QuestionDto(
            q.getId(),
            q.getQuestion(),
            optionRepo.findByQuestionId(q.getId()).stream()
                .map(o -> new TrainingDtos.QuizOptionDto(o.getId(), o.getText()))
                .toList()
        ))
        .toList();

    return new TrainingDtos.QuizDetail(quiz.getId(), quiz.getTitle(), quiz.getDescription(), quiz.getPolicy().getId(), questionDtos);
  }

  private static class PolicyAggregate {
    private final String policyTitle;
    private double totalScore = 0;
    private double totalMax = 0;
    private int count = 0;

    private PolicyAggregate(String policyTitle) {
      this.policyTitle = policyTitle;
    }

    private void accept(TrainingAttempt attempt) {
      totalScore += attempt.getScore();
      totalMax += attempt.getMaxScore();
      count++;
    }

    private double average() {
      return totalMax == 0 ? 0 : (totalScore / totalMax) * 100;
    }
  }

  private static class RoleAggregate {
    private final String role;
    private double totalScore = 0;
    private double totalMax = 0;
    private int count = 0;

    private RoleAggregate(String role) {
      this.role = role;
    }

    private void accept(TrainingAttempt attempt) {
      totalScore += attempt.getScore();
      totalMax += attempt.getMaxScore();
      count++;
    }

    private double average() {
      return totalMax == 0 ? 0 : (totalScore / totalMax) * 100;
    }
  }

  private double scorePercent(int score, int maxScore) {
    if (maxScore <= 0) {
      return 0;
    }
    return ((double) score / (double) maxScore) * 100;
  }
}
