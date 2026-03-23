-- Role type configurations
INSERT INTO role_type_config (role_type, default_weight, description) VALUES
    ('DEVELOPER', 1.00, 'Full-time developer contributing directly to deliverables'),
    ('TECH_LEAD', 0.80, 'Technical lead with 20% time allocated to leadership duties'),
    ('SCRUM_MASTER', 0.50, 'Scrum master with 50% time allocated to ceremony and process facilitation'),
    ('QA_ENGINEER', 1.00, 'Quality assurance engineer contributing directly to testing'),
    ('ARCHITECT', 0.70, 'Software architect with 30% time allocated to cross-team architecture work');

-- French public holidays for 2026 (recurring where applicable)
INSERT INTO public_holiday (date, name, locale, recurring) VALUES
    ('2026-01-01', 'Jour de l''An', 'FR', true),
    ('2026-04-06', 'Lundi de Pâques', 'FR', false),
    ('2026-05-01', 'Fête du Travail', 'FR', true),
    ('2026-05-08', 'Victoire 1945', 'FR', true),
    ('2026-05-14', 'Ascension', 'FR', false),
    ('2026-05-25', 'Lundi de Pentecôte', 'FR', false),
    ('2026-07-14', 'Fête Nationale', 'FR', true),
    ('2026-08-15', 'Assomption', 'FR', true),
    ('2026-11-01', 'Toussaint', 'FR', true),
    ('2026-11-11', 'Armistice', 'FR', true),
    ('2026-12-25', 'Noël', 'FR', true);

-- French public holidays for 2027
INSERT INTO public_holiday (date, name, locale, recurring) VALUES
    ('2027-01-01', 'Jour de l''An', 'FR', true),
    ('2027-03-29', 'Lundi de Pâques', 'FR', false),
    ('2027-05-01', 'Fête du Travail', 'FR', true),
    ('2027-05-08', 'Victoire 1945', 'FR', true),
    ('2027-05-06', 'Ascension', 'FR', false),
    ('2027-05-17', 'Lundi de Pentecôte', 'FR', false),
    ('2027-07-14', 'Fête Nationale', 'FR', true),
    ('2027-08-15', 'Assomption', 'FR', true),
    ('2027-11-01', 'Toussaint', 'FR', true),
    ('2027-11-11', 'Armistice', 'FR', true),
    ('2027-12-25', 'Noël', 'FR', true);
