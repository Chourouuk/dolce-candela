-- =====================================================================
--  Dolce Candela - Données de départ du catalogue
--  Les prix et stocks sont des EXEMPLES : à ajuster depuis l'espace admin.
--  (Aucun compte admin ici : il est créé au démarrage à partir des
--   variables d'environnement ADMIN_EMAIL / ADMIN_PASSWORD.)
-- =====================================================================

INSERT INTO categories (slug, name_fr, name_ar, name_en, description_fr, description_ar, description_en, display_order) VALUES
('bouquets-bougies',    'Bouquets de bougies',  'باقات الشموع', 'Candle Bouquets',
 'Bouquets de tulipes et compositions florales en cire.', 'باقات توليب وتنسيقات زهور من الشمع.', 'Tulip bouquets and floral wax arrangements.', 1),
('bougies-decoratives', 'Bougies décoratives',  'شموع للزينة',  'Decorative Candles',
 'Bougies sous cloche, pots décoratifs et créations originales.', 'شموع تحت قبة زجاجية، أواني مزخرفة وإبداعات أصلية.', 'Glass-dome candles, decorative jars and original creations.', 2),
('plateaux-decoratifs', 'Plateaux décoratifs',  'صواني للزينة', 'Decorative Trays',
 'Plateaux artisanaux pour décoration et cadeaux.', 'صواني مصنوعة يدويًا للزينة والهدايا.', 'Handmade trays for decoration and gifts.', 3),
('diffuseurs-parfumes', 'Diffuseurs parfumés',  'معطرات الجو',  'Scented Diffusers',
 'Diffuseurs pour parfumer et décorer les espaces.', 'معطرات لتعطير وتزيين المساحات.', 'Diffusers to scent and decorate your space.', 4);

INSERT INTO products (category_id, slug, name_fr, name_ar, name_en, description_fr, description_ar, description_en,
                      price, stock_quantity, made_to_order, customizable, badge) VALUES
((SELECT id FROM categories WHERE slug = 'bouquets-bougies'), 'bouquet-roses-mariee',
 'Bouquet de roses mariée', 'باقة ورود العروس', 'Rose Bridal Bouquet',
 'Composition de bougies en forme de roses aux tons rosés, idéale pour les mariages.',
 'تنسيق من شموع على شكل ورود بألوان وردية، مثالي للأعراس.',
 'Rose-shaped candles in soft blush tones, perfect for weddings.',
 89.000, 0, TRUE, TRUE, 'BESTSELLER'),
((SELECT id FROM categories WHERE slug = 'bouquets-bougies'), 'bouquet-tulipes',
 'Bouquet de tulipes', 'باقة التوليب', 'Tulip Bouquet',
 'Bouquet de tulipes en cire, couleurs au choix.', 'باقة توليب من الشمع بألوان حسب اختيارك.', 'Wax tulip bouquet in the colours of your choice.',
 65.000, 0, TRUE, TRUE, NULL),
((SELECT id FROM categories WHERE slug = 'bougies-decoratives'), 'coffret-naissance',
 'Coffret naissance', 'علبة هدية المولود', 'Newborn Gift Set',
 'Bougies pastel personnalisées avec le prénom du bébé.', 'شموع بألوان هادئة مخصصة باسم المولود.', 'Pastel candles personalised with the baby''s name.',
 55.000, 0, TRUE, TRUE, 'NEW'),
((SELECT id FROM categories WHERE slug = 'bougies-decoratives'), 'bougie-sauge-cedre',
 'Bougie Sauge & Cèdre', 'شمعة المريمية والأرز', 'Sage & Cedarwood Candle',
 'Bougie en pot céramique aux notes boisées.', 'شمعة في إناء خزفي بروائح خشبية.', 'Ceramic jar candle with woody notes.',
 42.000, 12, FALSE, FALSE, 'LIMITED'),
((SELECT id FROM categories WHERE slug = 'bougies-decoratives'), 'bougie-sous-cloche',
 'Bougie sous cloche', 'شمعة تحت قبة زجاجية', 'Glass Dome Candle',
 'Bougie parfumée ornée de fleurs en cire, sous cloche en verre.', 'شمعة معطرة مزينة بزهور من الشمع تحت قبة زجاجية.', 'Scented candle with wax flowers under a glass dome.',
 48.000, 8, FALSE, FALSE, NULL),
((SELECT id FROM categories WHERE slug = 'plateaux-decoratifs'), 'plateau-ovale-beige',
 'Plateau ovale beige', 'صينية بيضاوية بيج', 'Beige Oval Tray',
 'Plateau artisanal minimaliste avec touches dorées.', 'صينية يدوية بسيطة بلمسات ذهبية.', 'Minimalist handmade tray with gold accents.',
 35.000, 10, FALSE, TRUE, NULL),
((SELECT id FROM categories WHERE slug = 'diffuseurs-parfumes'), 'diffuseur-botanique',
 'Diffuseur botanique', 'معطر جو نباتي', 'Botanical Reed Diffuser',
 'Diffuseur à bâtonnets décoré de fleurs séchées.', 'معطر بأعواد مزين بزهور مجففة.', 'Reed diffuser decorated with dried flowers.',
 39.000, 15, FALSE, TRUE, NULL);

-- Couleurs (reprises du formulaire « Custom Order » du prototype)
INSERT INTO customization_options (option_type, code, label_fr, label_ar, label_en, hex_color, extra_price) VALUES
('COLOR', 'creme',      'Crème',        'كريمي',       'Cream',      '#F5E6D3', 0),
('COLOR', 'peche',      'Pêche',        'خوخي',        'Peach',      '#FADBD0', 0),
('COLOR', 'or',         'Doré',         'ذهبي',        'Gold',       '#C9A96E', 0),
('COLOR', 'beige',      'Beige',        'بيج',         'Beige',      '#E8D5B9', 0),
('COLOR', 'vieux-rose', 'Vieux rose',   'وردي عتيق',   'Dusty rose', '#D4A0A0', 0),
('COLOR', 'sauge',      'Vert sauge',   'أخضر مريمية', 'Sage green', '#A8C8A0', 0),
('COLOR', 'bleu-ciel',  'Bleu pastel',  'أزرق فاتح',   'Pastel blue','#B0C0E8', 0),
('COLOR', 'lilas',      'Lilas',        'ليلكي',       'Lilac',      '#D8B8E8', 0),
('COLOR', 'chocolat',   'Chocolat',     'بني داكن',    'Chocolate',  '#2B2118', 0);

INSERT INTO customization_options (option_type, code, label_fr, label_ar, label_en, extra_price) VALUES
('SCENT', 'sans-parfum',    'Sans parfum',     'بدون عطر',   'Unscented',       0),
('SCENT', 'vanille',        'Vanille',         'فانيليا',    'Vanilla',         0),
('SCENT', 'rose',           'Rose',            'ورد',        'Rose',            0),
('SCENT', 'jasmin',         'Jasmin',          'ياسمين',     'Jasmine',         0),
('SCENT', 'fleur-oranger',  'Fleur d''oranger','زهر',        'Orange blossom',  0),
('SCENT', 'lavande',        'Lavande',         'خزامى',      'Lavender',        0),
('SIZE',  'petit',          'Petit',           'صغير',       'Small',           0),
('SIZE',  'moyen',          'Moyen',           'متوسط',      'Medium',          10.000),
('SIZE',  'grand',          'Grand',           'كبير',       'Large',           20.000),
('PACKAGING', 'boite-cadeau', 'Boîte cadeau',  'علبة هدية',  'Gift box',        5.000),
('PACKAGING', 'ruban-satin',  'Ruban satin',   'شريط ساتان', 'Satin ribbon',    2.000);

-- Produits personnalisables : couleurs + parfums + emballages
INSERT INTO product_customization_options (product_id, option_id)
SELECT p.id, o.id
FROM products p CROSS JOIN customization_options o
WHERE p.customizable = TRUE
  AND o.option_type IN ('COLOR', 'SCENT', 'PACKAGING');

-- Les bouquets ont en plus le choix de la taille
INSERT INTO product_customization_options (product_id, option_id)
SELECT p.id, o.id
FROM products p
JOIN categories c ON c.id = p.category_id
CROSS JOIN customization_options o
WHERE c.slug = 'bouquets-bougies'
  AND o.option_type = 'SIZE';

-- Images (chemins à remplacer par les vraies photos)
INSERT INTO product_images (product_id, url, alt_text, position, is_main)
SELECT id, '/images/products/' || slug || '.jpg', name_fr, 0, TRUE FROM products;
