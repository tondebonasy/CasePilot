# CasePilot UI

The Angular application uses standalone components, reactive forms, and Angular Material.

## Styling

Shared colors, typography, spacing, and elevation are defined in `src/app/_theme.scss`. Global Material styling lives in `src/styles.scss`; page styles are scoped to their components.

## Brand assets

Use `src/assets/casepilot-mark.svg` for the application logo and favicon. Keep product labels bank-neutral and refer to payment investigations as disputes or cases.

## Layout

The login page uses two columns on larger screens and a compact header above the form on phones. Error messages remain visible in the form. At extreme zoom levels or when the software keyboard is open, scrolling remains available so controls can be reached.

## Verification

Run `npm run build -- --configuration production` after changes. Check sign-in validation, password visibility, navigation, case filtering, and CSV downloads against the backend.
