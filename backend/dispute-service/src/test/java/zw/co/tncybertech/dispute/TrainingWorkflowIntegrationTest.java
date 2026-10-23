package zw.co.tncybertech.dispute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zw.co.tncybertech.dispute.api.dto.TrainingDtos;
import zw.co.tncybertech.dispute.domain.training.RbzPolicy;
import zw.co.tncybertech.dispute.domain.training.RbzQuiz;
import zw.co.tncybertech.dispute.domain.training.RbzQuizOption;
import zw.co.tncybertech.dispute.domain.training.RbzQuizQuestion;
import zw.co.tncybertech.dispute.repo.RbzPolicyRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizOptionRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizQuestionRepository;
import zw.co.tncybertech.dispute.repo.RbzQuizRepository;
import zw.co.tncybertech.dispute.repo.TrainingAnswerRepository;
import zw.co.tncybertech.dispute.repo.TrainingAttemptRepository;
import zw.co.tncybertech.dispute.service.TrainingService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class TrainingWorkflowIntegrationTest {

  @Autowired
  private TrainingService trainingService;

  @Autowired
  private RbzPolicyRepository policyRepo;

  @Autowired
  private RbzQuizRepository quizRepo;

  @Autowired
  private RbzQuizQuestionRepository questionRepo;

  @Autowired
  private RbzQuizOptionRepository optionRepo;

  @Autowired
  private TrainingAttemptRepository attemptRepo;

  @Autowired
  private TrainingAnswerRepository answerRepo;

  @Test
  void submitAttemptScoresAndUpdatesMetrics() {
    RbzPolicy policy = policyRepo.save(RbzPolicy.builder()
        .title("Integration RBZ Policy")
        .circularReference("NPS/01/2026")
        .summary("Integration summary")
        .effectiveDate(LocalDate.now())
        .keyRequirements("Test reqs")
        .active(true)
        .build());

    RbzQuiz quiz = quizRepo.save(RbzQuiz.builder()
        .policy(policy)
        .title("Integration Quiz")
        .description("Verify scoring")
        .build());

    RbzQuizQuestion q1 = questionRepo.save(RbzQuizQuestion.builder()
        .quiz(quiz)
        .question("What is expected?")
        .explanation("Expect us to behave.")
        .build());

    RbzQuizQuestion q2 = questionRepo.save(RbzQuizQuestion.builder()
        .quiz(quiz)
        .question("What is forbidden?")
        .explanation("Bad behavior is forbidden.")
        .build());

    RbzQuizOption correctOption = optionRepo.save(RbzQuizOption.builder()
        .question(q1)
        .text("Follow the policy")
        .correct(true)
        .build());

    optionRepo.save(RbzQuizOption.builder()
        .question(q1)
        .text("Ignore it")
        .correct(false)
        .build());

    RbzQuizOption incorrectOptionQ2 = optionRepo.save(RbzQuizOption.builder()
        .question(q2)
        .text("Blame others")
        .correct(false)
        .build());

    optionRepo.save(RbzQuizOption.builder()
        .question(q2)
        .text("Document and escalate")
        .correct(true)
        .build());

    long beforeAnswers = answerRepo.count();

    TrainingDtos.SubmitAttemptRequest request = new TrainingDtos.SubmitAttemptRequest(
        quiz.getId(),
        List.of(
            new TrainingDtos.Answer(q1.getId(), correctOption.getId()),
            new TrainingDtos.Answer(q2.getId(), incorrectOptionQ2.getId())
        )
    );

    TrainingDtos.AttemptResult result = trainingService.submitAttempt(3L, "AGENT", request);
    assertEquals(1, result.score());
    assertEquals(2, result.maxScore());
    assertEquals(1, result.weaknesses().size());
    assertEquals(beforeAnswers + 2, answerRepo.count());

    assertEquals(1, attemptRepo.findByUserIdOrderByCreatedAtDesc(3L).size());

    Instant from = Instant.now().minusSeconds(60);
    Instant to = Instant.now().plusSeconds(60);
    TrainingDtos.AwarenessMetrics metrics = trainingService.awarenessMetrics(from, to);
    assertEquals(50.0, metrics.averageScore());
    assertEquals(1, metrics.completionCount());
    assertEquals(1, metrics.byPolicy().stream()
        .filter(entry -> entry.policyId().equals(policy.getId()))
        .findFirst()
        .map(TrainingDtos.PolicyAwareness::attempts)
        .orElse(0L));
  }
}
