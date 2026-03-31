-- Remove locale from public_holiday: holidays are now global (no locale filtering)
ALTER TABLE public_holiday DROP CONSTRAINT IF EXISTS public_holiday_date_locale_key;
ALTER TABLE public_holiday DROP COLUMN IF EXISTS locale;
ALTER TABLE public_holiday ADD CONSTRAINT public_holiday_date_key UNIQUE (date);
