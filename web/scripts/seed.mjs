/**
 * Creates the schema and seeds demonstration data.
 *
 * Run once against a fresh Neon branch:
 *   npm run db:seed
 *
 * Safe to re-run: every statement is idempotent and existing rows are left alone.
 * The schema matches what the Java desktop client creates, so both editions share
 * one database.
 */

import { pbkdf2, randomBytes } from "node:crypto";
import { promisify } from "node:util";

import { connect } from "./connection.mjs";

const pbkdf2Async = promisify(pbkdf2);

const sql = connect("db:seed");

/**
 * Append the ten demo loan scenarios even when the register already has rows.
 *   npm run db:seed -- --scenarios
 * Without it, an existing register is left untouched.
 */
const SEED_SCENARIOS = process.argv.includes("--scenarios");

// Must match src/lib/password.ts and Security.java.
const ITERATIONS = 210_000;

async function hash(password) {
  const salt = randomBytes(16);
  const derived = await pbkdf2Async(password, salt, ITERATIONS, 32, "sha256");
  return `pbkdf2$${ITERATIONS}$${salt.toString("base64url")}$${derived.toString("base64url")}`;
}

const TABLES = [
  `CREATE TABLE IF NOT EXISTS livre (
     id_livre SERIAL PRIMARY KEY, titre VARCHAR(255) NOT NULL, auteur VARCHAR(255) NOT NULL,
     genre VARCHAR(100), resume_livre TEXT, disponibilite BOOLEAN DEFAULT TRUE,
     image_url TEXT)`,
  `CREATE TABLE IF NOT EXISTS utilisateur (
     id_utilisateur SERIAL PRIMARY KEY, nom VARCHAR(255) NOT NULL, motDePasse VARCHAR(255),
     numero INTEGER, email VARCHAR(255), role_utilisateur VARCHAR(50))`,
  `CREATE TABLE IF NOT EXISTS admin (
     id_admin SERIAL PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE)`,
  `CREATE TABLE IF NOT EXISTS lecteur (
     id_lecteur SERIAL PRIMARY KEY, id_utilisateur INTEGER REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE)`,
  `CREATE TABLE IF NOT EXISTS emprunt (
     id_emprunt SERIAL PRIMARY KEY, id_utilisateur INTEGER, id_livre INTEGER,
     dateEmprunts DATE, dateRetour DATE, date_retour_livre DATE)`,
  `CREATE TABLE IF NOT EXISTS reservation (
     id_reservation SERIAL PRIMARY KEY, id_utilisateur INTEGER, dateReservation DATE)`,
];

// Each is optional: a failure means it is already applied, or existing data blocks it.
const MIGRATIONS = [
  `ALTER TABLE emprunt ADD COLUMN IF NOT EXISTS date_retour_livre DATE`,
  // Cover artwork, resolved from Open Library / Google Books by `npm run db:covers`.
  // Null means "no artwork found" — both clients then draw the generated gradient.
  `ALTER TABLE livre ADD COLUMN IF NOT EXISTS image_url TEXT`,
  `ALTER TABLE emprunt ADD CONSTRAINT emprunt_livre_fk
     FOREIGN KEY (id_livre) REFERENCES livre(id_livre) ON DELETE CASCADE`,
  `ALTER TABLE emprunt ADD CONSTRAINT emprunt_user_fk
     FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE`,
  `ALTER TABLE reservation ADD CONSTRAINT reservation_user_fk
     FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id_utilisateur) ON DELETE CASCADE`,
  `CREATE UNIQUE INDEX IF NOT EXISTS utilisateur_nom_unique ON utilisateur (LOWER(nom))`,
  `CREATE INDEX IF NOT EXISTS emprunt_livre_idx ON emprunt (id_livre)`,
  `CREATE INDEX IF NOT EXISTS emprunt_user_idx ON emprunt (id_utilisateur)`,
  `CREATE INDEX IF NOT EXISTS reservation_user_idx ON reservation (id_utilisateur)`,
  `CREATE INDEX IF NOT EXISTS livre_titre_idx ON livre (LOWER(titre))`,
  `CREATE INDEX IF NOT EXISTS livre_auteur_idx ON livre (LOWER(auteur))`,
];

const BOOKS = [
  ["Antigone", "Jean Anouilh", "Tragédie",
   "Antigone défie le roi Créon en enterrant son frère Polynice au nom des lois divines et morales.", false,
   "https://covers.openlibrary.org/b/id/116843-L.jpg"],
  ["Le Dernier Jour d'un Condamné", "Victor Hugo", "Roman à thèse",
   "Un réquisitoire contre la peine de mort à travers les dernières heures d'un condamné anonyme.", false,
   "https://covers.openlibrary.org/b/id/998791-L.jpg"],
  ["L'Étranger", "Albert Camus", "Roman",
   "Meursault, indifférent à la société, commet un meurtre absurde sous le soleil d'Alger.", false,
   "https://covers.openlibrary.org/b/id/13151269-L.jpg"],
  ["Madame Bovary", "Gustave Flaubert", "Roman",
   "Emma Bovary cherche dans des liaisons romantiques un moyen d'échapper à la monotonie de sa vie provinciale.", false,
   "https://covers.openlibrary.org/b/id/12993424-L.jpg"],
  ["Le Petit Prince", "Antoine de Saint-Exupéry", "Roman",
   "Un conte philosophique sur l'amitié, l'amour et l'innocence perçus à travers le voyage d'un petit prince.", false,
   "https://covers.openlibrary.org/b/id/10708272-L.jpg"],
  ["Voyage au bout de la nuit", "Louis-Ferdinand Céline", "Roman",
   "Bardamu parcourt la Première Guerre mondiale, l'Afrique coloniale, l'Amérique et la banlieue parisienne.", true,
   "https://covers.openlibrary.org/b/id/14054027-L.jpg"],
  ["Les Liaisons dangereuses", "Choderlos de Laclos", "Roman",
   "Roman épistolaire retraçant les rivalités libertines de la marquise de Merteuil et du vicomte de Valmont.", true,
   "https://covers.openlibrary.org/b/id/5258265-L.jpg"],
  ["Cent ans de solitude", "Gabriel García Márquez", "Roman",
   "L'histoire de la famille Buendía dans le village imaginaire de Macondo sur sept générations.", true,
   "https://covers.openlibrary.org/b/id/10879677-L.jpg"],
  ["Gatsby le Magnifique", "F. Scott Fitzgerald", "Roman",
   "Dans les années 1920, Jay Gatsby organise des fêtes somptueuses pour reconquérir son amour de jeunesse Daisy.", true,
   "https://covers.openlibrary.org/b/id/13310378-L.jpg"],
  ["Don Quichotte", "Miguel de Cervantes", "Roman",
   "Un pauvre hidalgo espagnol, passionné par les romans de chevalerie, parcourt l'Espagne pour combattre le mal.", true,
   "https://covers.openlibrary.org/b/id/13342356-L.jpg"],
  ["L'Alchimiste", "Paulo Coelho", "Roman",
   "Santiago, un jeune berger andalou, entreprend un voyage en Égypte à la recherche de sa Légende Personnelle.", true,
   "https://covers.openlibrary.org/b/id/994197-L.jpg"],
  ["L'Amie prodigieuse", "Elena Ferrante", "Roman",
   "L'amitié complexe entre deux filles, Elena et Lila, nées dans un quartier pauvre de Naples dans les années 1950.", true,
   "https://covers.openlibrary.org/b/id/12706346-L.jpg"],
  ["Americanah", "Chimamanda Ngozi Adichie", "Roman",
   "L'histoire d'amour et de déracinement d'Ifemelu et Obinze entre le Nigeria, les États-Unis et le Royaume-Uni.", true,
   "https://covers.openlibrary.org/b/id/8474037-L.jpg"],
  ["Le Rouge et le Noir", "Stendhal", "Roman",
   "L'ambition sociale du jeune Julien Sorel dans la France de la Restauration, partagé entre l'église et l'armée.", true,
   "https://covers.openlibrary.org/b/id/8231413-L.jpg"],
  ["La Peste", "Albert Camus", "Roman",
   "La lutte des habitants d'Oran frappés par une épidémie de peste, métaphore de la résistance humaine.", true,
   "https://covers.openlibrary.org/b/id/13151272-L.jpg"],
  ["Germinal", "Émile Zola", "Roman",
   "La grève des mineurs de Montsou menée par Étienne Lantier sous le Second Empire.", true,
   "https://covers.openlibrary.org/b/id/8236935-L.jpg"],
  ["Bel-Ami", "Guy de Maupassant", "Roman",
   "L'ascension sociale de Georges Duroy, homme ambitieux et séducteur dans le monde du journalisme parisien.", true,
   "https://covers.openlibrary.org/b/id/997432-L.jpg"],
  ["La Nausée", "Jean-Paul Sartre", "Roman",
   "Antoine Roquentin prend conscience de l'absurdité fondamentale de la réalité et de l'existence.", true,
   "https://covers.openlibrary.org/b/id/9393973-L.jpg"],
  ["Le Père Goriot", "Honoré de Balzac", "Roman",
   "La dévotion d'un père ruiné pour ses filles ambitieuses dans une pension parisienne du XIXe siècle.", true,
   "https://covers.openlibrary.org/b/id/15156928-L.jpg"],
  ["Sido", "Colette", "Roman",
   "Un hommage poétique et délicat de l'auteure à sa mère Sido et au jardin de son enfance en Bourgogne.", true,
   "https://covers.openlibrary.org/b/id/2188657-L.jpg"],
  ["L'Écume des jours", "Boris Vian", "Roman",
   "L'histoire d'amour poétique et tragique de Colin et Chloé, atteinte d'un nénuphar dans le poumon.", true,
   "https://covers.openlibrary.org/b/id/3169856-L.jpg"],
  ["Candide", "Voltaire", "Roman à thèse",
   "Un conte philosophique dénonçant l'optimisme béat à travers les mésaventures de Candide à travers le monde.", true,
   "https://covers.openlibrary.org/b/id/12736044-L.jpg"],
  ["Zadig", "Voltaire", "Roman à thèse",
   "Les tribulations d'un jeune homme sage à Babylone illustrant la Providence et le destin humain.", true,
   "https://covers.openlibrary.org/b/id/3072291-L.jpg"],
  ["La Ferme des animaux", "George Orwell", "Roman à thèse",
   "Une satire allégorique du stalinisme dans laquelle les animaux d'une ferme prennent le pouvoir.", true,
   "https://covers.openlibrary.org/b/id/13147152-L.jpg"],
  ["1984", "George Orwell", "Roman à thèse",
   "Une dystopie effrayante sur un régime totalitaire omniscience contrôlé par Big Brother.", true,
   "https://covers.openlibrary.org/b/id/8745958-L.jpg"],
  ["Le Meilleur des mondes", "Aldous Huxley", "Roman à thèse",
   "Une société future eugéniste où le bonheur artificiel et le conditionnement suppriment la liberté.", true,
   "https://covers.openlibrary.org/b/id/9382676-L.jpg"],
  ["L'Ingénu", "Voltaire", "Roman à thèse",
   "Un Huron débarque en France et confronte avec candeur les abus de la religion et du pouvoir politique.", true,
   "https://covers.openlibrary.org/b/id/965252-L.jpg"],
  ["Lettres persanes", "Montesquieu", "Roman à thèse",
   "La critique de la société française sous la Régence vue à travers les yeux de deux voyageurs persans.", true,
   "https://covers.openlibrary.org/b/id/6841885-L.jpg"],
  ["Jacques le Fataliste", "Denis Diderot", "Roman à thèse",
   "Un dialogue philosophique sur le libre arbitre entre Jacques et son maître lors d'un voyage.", true,
   "https://covers.openlibrary.org/b/id/246568-L.jpg"],
  ["Rhinocéros", "Eugène Ionesco", "Roman à thèse",
   "Une allégorie dramatique de la montée du fanatisme où tous les habitants se transforment en rhinocéros.", true,
   "https://covers.openlibrary.org/b/id/9678786-L.jpg"],
  ["La Chute", "Albert Camus", "Roman à thèse",
   "Jean-Baptiste Clamence livre une confession cynique sur la culpabilité et le jugement moral dans un bar d'Amsterdam.", true,
   "https://covers.openlibrary.org/b/id/8296477-L.jpg"],
  ["Le Horla", "Guy de Maupassant", "Roman à thèse",
   "Un journal intime décrivant la descente dans la folie d'un homme hanté par une présence invisible.", true,
   "https://covers.openlibrary.org/b/id/4859675-L.jpg"],
  ["Sa Majesté des mouches", "William Golding", "Roman à thèse",
   "Des enfants naufragés sur une île déserte retournent rapidement à la sauvagerie primaire.", true,
   "https://covers.openlibrary.org/b/id/967627-L.jpg"],
  ["L'Île des pingouins", "Anatole France", "Roman à thèse",
   "Une satire de l'histoire de la France représentée par une colonie de pingouins accidentellement baptisés.", true,
   "https://covers.openlibrary.org/b/id/7299050-L.jpg"],
  ["Manon Lescaut", "Abbé Prévost", "Roman à thèse",
   "La passion dévorante et la déchéance morale du chevalier des Grieux pour la charmante Manon.", true,
   "https://covers.openlibrary.org/b/id/8236918-L.jpg"],
  ["Claude Gueux", "Victor Hugo", "Roman à thèse",
   "Un plaidoyer social sur la pauvreté, la justice pénale et la nécessité d'éduquer le peuple.", true,
   "https://covers.openlibrary.org/b/id/2140567-L.jpg"],
  ["Le Grand Meaulnes", "Alain-Fournier", "Roman à thèse",
   "La quête nostalgique d'un domaine mystérieux et de l'amour de jeunesse par Augustin Meaulnes.", true,
   "https://covers.openlibrary.org/b/id/8239494-L.jpg"],
  ["Soumission", "Michel Houellebecq", "Roman à thèse",
   "Une politique-fiction d'anticipation explorant les transformations de la société française contemporaine.", true,
   "https://covers.openlibrary.org/b/id/7347449-L.jpg"],
  ["Phèdre", "Jean Racine", "Tragédie",
   "La passion incestueuse et dévorante de Phèdre pour son beau-fils Hippolyte provoque le drame.", true,
   "https://covers.openlibrary.org/b/id/10795931-L.jpg"],
  ["Andromaque", "Jean Racine", "Tragédie",
   "Un dilemme amoureux et politique entre Pyrrhus, Andromaque, Oreste et Hermione après la guerre de Troie.", true,
   "https://covers.openlibrary.org/b/id/8231426-L.jpg"],
  ["Horace", "Pierre Corneille", "Tragédie",
   "Le devoir patriotique pousse Horace à affronter au combat les Curiaces, la famille de son épouse.", true,
   "https://covers.openlibrary.org/b/id/8247737-L.jpg"],
  ["Britannicus", "Jean Racine", "Tragédie",
   "L'émergence de la tyrannie chez le jeune empereur Néron luttant contre l'influence de sa mère Agrippine.", true,
   "https://covers.openlibrary.org/b/id/2012615-L.jpg"],
  ["Le Cid", "Pierre Corneille", "Tragédie",
   "Rodrigue et Chimène doivent choisir entre leur amour et l'honneur exigeant de leurs familles respectives.", true,
   "https://covers.openlibrary.org/b/id/8236984-L.jpg"],
  ["Iphigénie", "Jean Racine", "Tragédie",
   "Le roi Agamemnon doit sacrifier sa fille Iphigénie pour obtenir les vents favorables vers Troie.", true,
   "https://covers.openlibrary.org/b/id/5823699-L.jpg"],
  ["Bérénice", "Jean Racine", "Tragédie",
   "L'empereur Titus renonce par devoir d'État à épouser Bérénice, la reine de Judée qu'il aime.", true,
   "https://covers.openlibrary.org/b/id/3124481-L.jpg"],
  ["Athalie", "Jean Racine", "Tragédie",
   "La reine impie Athalie tente d'exterminer la lignée des rois de Juda avant d'être détronée par Joas.", true,
   "https://covers.openlibrary.org/b/id/5754451-L.jpg"],
  ["Œdipe Roi", "Sophocle", "Tragédie",
   "Œdipe cherche le meurtrier du roi Laios avant de découvrir qu'il a accompli la prophétie tragique.", true,
   "https://covers.openlibrary.org/b/id/10792529-L.jpg"],
  ["Électre", "Sophocle", "Tragédie",
   "Électre attend désespérément le retour de son frère Oreste pour venger la mort de leur père Agamemnon.", true,
   "https://covers.openlibrary.org/b/id/10597561-L.jpg"],
  ["Hamlet", "William Shakespeare", "Tragédie",
   "Le prince du Danemark feint la folie pour venger l'assassinat de son père par son oncle Claudius.", true,
   "https://covers.openlibrary.org/b/id/8281954-L.jpg"],
  ["Macbeth", "William Shakespeare", "Tragédie",
   "Poussé par les sorcières et sa femme, le général Macbeth s'empare violemment du trône d'Écosse.", true,
   "https://covers.openlibrary.org/b/id/872432-L.jpg"],
  ["Roméo et Juliette", "William Shakespeare", "Tragédie",
   "L'amour passionné et maudit entre deux jeunes gens issus des familles rivales de Vérone.", true,
   "https://covers.openlibrary.org/b/id/13335428-L.jpg"],
  ["Othello", "William Shakespeare", "Tragédie",
   "Manipulé par l'amiral Iago, le général maure Othello est dévoré par une jalousie destructrice.", true,
   "https://covers.openlibrary.org/b/id/7165018-L.jpg"],
  ["Le Roi Lear", "William Shakespeare", "Tragédie",
   "Un vieux roi divise son royaume entre ses filles hypocrites et rejette la seule qui l'aime sincèrement.", true,
   "https://covers.openlibrary.org/b/id/7420452-L.jpg"],
  ["Médée", "Euripide", "Tragédie",
   "Trahie par Jason qui épouse la fille du roi Créon, Médée accomplit une vengeance terrible.", true,
   "https://covers.openlibrary.org/b/id/10538430-L.jpg"],
  ["Lorenzaccio", "Alfred de Musset", "Tragédie",
   "Lorenzo de Médicis se dévoue à la tyrannie pour assassiner le duc Alexandre et libérer Florence.", true,
   "https://covers.openlibrary.org/b/id/2140539-L.jpg"],
  ["Ruy Blas", "Victor Hugo", "Tragédie",
   "Un simple valet amoureux de la reine d'Espagne est utilisé dans une intrigue politique vengeance.", true,
   "https://covers.openlibrary.org/b/id/8245386-L.jpg"],
  ["Hernani", "Victor Hugo", "Tragédie",
   "L'amour entre le noble bandit Hernani et Doña Sol contrarié par le roi Don Carlos et le duc Silva.", true,
   "https://covers.openlibrary.org/b/id/10240221-L.jpg"],
  ["Enfance", "Nathalie Sarraute", "Autobiographie",
   "Une série de souvenirs fragmentés et dialogués explorant la mémoire d'une enfance franco-russe.", true,
   "https://covers.openlibrary.org/b/id/966116-L.jpg"],
  ["Les Confessions", "Jean-Jacques Rousseau", "Autobiographie",
   "L'auteur s'engage à présenter la vérité absolue sur sa vie, ses vertus et ses faiblesses.", true,
   "https://covers.openlibrary.org/b/id/6371045-L.jpg"],
  ["Mémoires d'outre-tombe", "François-René de Chateaubriand", "Autobiographie",
   "Une fresque mémorielle personnelle et historique traversant la Révolution, l'Empire et la Restauration.", true,
   "https://covers.openlibrary.org/b/id/2138503-L.jpg"],
  ["Un sac de billes", "Joseph Joffo", "Autobiographie",
   "La fuite tragique et courageuse de deux jeunes frères juifs à travers la France occupée.", true,
   "https://covers.openlibrary.org/b/id/976742-L.jpg"],
  ["Mémoires d'une jeune fille rangée", "Simone de Beauvoir", "Autobiographie",
   "La jeunesse et l'émancipation intellectuelle d'une jeune femme de la bourgeoisie parisienne.", true,
   "https://covers.openlibrary.org/b/id/37132-L.jpg"],
  ["L'Amant", "Marguerite Duras", "Autobiographie",
   "Le récit poétique et autofictionnel d'un amour de jeunesse en Indochine française.", true,
   "https://covers.openlibrary.org/b/id/5401955-L.jpg"],
  ["Journal d'Anne Frank", "Anne Frank", "Autobiographie",
   "Le journal intime d'une jeune fille juive cachée à Amsterdam avec sa famille pendant la Seconde Guerre mondiale.", true,
   "https://covers.openlibrary.org/b/id/8584021-L.jpg"],
  ["La Gloire de mon père", "Marcel Pagnol", "Autobiographie",
   "Les souvenirs chaleureux des vacances d'été dans les collines de Provence avec sa famille.", true,
   "https://covers.openlibrary.org/b/id/11410086-L.jpg"],
  ["Le Château de ma mère", "Marcel Pagnol", "Autobiographie",
   "La suite des souvenirs provençaux retraçant les traversées secrètes des grands domaines.", true,
   "https://covers.openlibrary.org/b/id/11667012-L.jpg"],
  ["La Vie devant soi", "Romain Gary", "Autobiographie",
   "Le petit Momo, orphelin arabe, grandit à Belleville chez Madame Rosa, ancienne déportée.", true,
   "https://covers.openlibrary.org/b/id/10374575-L.jpg"],
  ["L'Africain", "J.M.G. Le Clézio", "Autobiographie",
   "L'auteur se remémore son père médecin au Nigeria et sa découverte émerveillée du continent africain.", true,
   "https://covers.openlibrary.org/b/id/2162169-L.jpg"],
  ["La Promesse de l'aube", "Romain Gary", "Autobiographie",
   "Le roman autobiographique retraçant l'amour inconditionnel et les grandes ambitions d'une mère pour son fils.", true,
   "https://covers.openlibrary.org/b/id/538926-L.jpg"],
  ["Les Rêveries du promeneur solitaire", "Jean-Jacques Rousseau", "Autobiographie",
   "Dix méditations philosophiques rédigées lors des dernières promenades de l'auteur autour de Paris.", true,
   "https://covers.openlibrary.org/b/id/8245265-L.jpg"],
  ["Stupeur et Tremblements", "Amélie Nothomb", "Autobiographie",
   "Le récit satirique du stage vécu par une jeune Belge au sein d'une grande entreprise japonaise.", true,
   "https://covers.openlibrary.org/b/id/177795-L.jpg"],
  ["Lambeaux", "Charles Juliet", "Autobiographie",
   "Un double hommage poignant rendu à sa mère biologique disparue et à sa mère adoptive.", true,
   "https://covers.openlibrary.org/b/id/2181209-L.jpg"],
  ["La Statue de sel", "Albert Memmi", "Autobiographie",
   "L'itinéraire d'un jeune homme juif tunisien à la recherche de son identité culturelle.", true,
   "https://covers.openlibrary.org/b/id/967148-L.jpg"],
  ["Les Misérables", "Victor Hugo", "Roman historique",
   "La rédemption de Jean Valjean et la fresque sociale des miséreux de Paris au XIXe siècle.", true,
   "https://covers.openlibrary.org/b/id/12721865-L.jpg"],
  ["Notre-Dame de Paris", "Victor Hugo", "Roman historique",
   "Le destin tragique d'Esmeralda, de Quasimodo et de Frollo autour de la cathédrale médiévale.", true,
   "https://covers.openlibrary.org/b/id/2626880-L.jpg"],
  ["La Reine Margot", "Alexandre Dumas", "Roman historique",
   "Intrigues de cour, passion et horreurs du massacre de la Saint-Barthélemy sous Charles IX.", true,
   "https://covers.openlibrary.org/b/id/14557277-L.jpg"],
  ["Les Rois maudits", "Maurice Druon", "Roman historique",
   "La malédiction des Templiers s'abattant sur la dynastie des Capétiens directs du XIVe siècle.", true,
   "https://covers.openlibrary.org/b/id/13790132-L.jpg"],
  ["Salammbô", "Gustave Flaubert", "Roman historique",
   "La guerre des Mercenaires contre Carthage et la passion tragique de Mâtho pour Salammbô.", true,
   "https://covers.openlibrary.org/b/id/3078356-L.jpg"],
  ["Au revoir là-haut", "Pierre Lemaitre", "Roman historique",
   "Deux démobilisés de la Grande Guerre organisent une arnaque monumentale aux monuments aux morts.", true,
   "https://covers.openlibrary.org/b/id/8434974-L.jpg"],
  ["Le Nom de la rose", "Umberto Eco", "Roman historique",
   "En 1327, le moine Guillaume de Baskerville enquête sur des crimes mystérieux dans une abbaye bénédicte.", true,
   "https://covers.openlibrary.org/b/id/10490796-L.jpg"],
  ["La Voleuse de livres", "Markus Zusak", "Roman historique",
   "Pendant la Seconde Guerre mondiale en Allemagne, la jeune Liesel trouve du réconfort en volant des livres.", true,
   "https://covers.openlibrary.org/b/id/8153054-L.jpg"],
  ["Quo Vadis", "Henryk Sienkiewicz", "Roman historique",
   "L'amour d'un patricien romain et d'une jeune chrétienne sous le règne tyrannique de Néron.", true,
   "https://covers.openlibrary.org/b/id/833096-L.jpg"],
  ["Mémoires de Hadrien", "Marguerite Yourcenar", "Roman historique",
   "L'empereur Hadrien médite sur sa vie, son pouvoir et sa passion pour Antinoüs sous forme de lettre.", false,
   "https://covers.openlibrary.org/b/id/9687045-L.jpg"],
  ["Les Trois Mousquetaires", "Alexandre Dumas", "Roman historique",
   "D'Artagnan rejoint Athos, Porthos et Aramis pour déjouer les complots du cardinal de Richelieu.", true,
   "https://covers.openlibrary.org/b/id/11929973-L.jpg"],
  ["Vingt Ans après", "Alexandre Dumas", "Roman historique",
   "Les quatre mousquetaires se retrouvent au temps de la Fronde et de la Révolution anglaise.", true,
   "https://covers.openlibrary.org/b/id/14564526-L.jpg"],
  ["Les Chouans", "Honoré de Balzac", "Roman historique",
   "L'insurrection royaliste en Bretagne sous la Révolution et l'amour entre un chef chouan et une espionne.", true,
   "https://covers.openlibrary.org/b/id/3092494-L.jpg"],
  ["Le Chevalier de Maison-Rouge", "Alexandre Dumas", "Roman historique",
   "Un complot royaliste tente de faire évader la reine Marie-Antoinette sous la Terreur.", true,
   "https://covers.openlibrary.org/b/id/6321688-L.jpg"],
  ["L'Allée du Roi", "Françoise Chandernagor", "Roman historique",
   "Les mémoires fictifs de Madame de Maintenon, épouse secrète du Roi-Soleil Louis XIV.", true,
   "https://covers.openlibrary.org/b/id/10407762-L.jpg"],
  ["Le Comte de Monte-Cristo", "Alexandre Dumas", "Roman historique",
   "Injustement emprisonné au château d'If, Edmond Dantès s'évade pour accomplir une vengeance méthodique.", true,
   "https://covers.openlibrary.org/b/id/14561715-L.jpg"],
  ["Harry Potter à l'école des sorciers", "J.K. Rowling", "Fantastique",
   "Harry découvre son héritage magique et fait ses premiers pas à l'école de sorcellerie Poudlard.", true,
   "https://covers.openlibrary.org/b/id/15155833-L.jpg"],
  ["Le Seigneur des Anneaux", "J.R.R. Tolkien", "Fantastique",
   "Frodon Sacquet entreprend un voyage périlleux pour détruire l'Anneau Unique dans la Montagne du Destin.", true,
   "https://covers.openlibrary.org/b/id/14627060-L.jpg"],
  ["Le Hobbit", "J.R.R. Tolkien", "Fantastique",
   "Bilbon Sacquet accompagne treize nains et le mage Gandalf pour reprendre le trésor gardé par le dragon Smaug.", true,
   "https://covers.openlibrary.org/b/id/14627509-L.jpg"],
  ["Le Silmarillion", "J.R.R. Tolkien", "Fantastique",
   "Les mythes et légendes du Premier Âge de la Terre du Milieu avant les événements du Seigneur des Anneaux.", true,
   "https://covers.openlibrary.org/b/id/8762940-L.jpg"],
  ["Le Trône de Fer", "George R.R. Martin", "Fantastique",
   "Des familles nobles rivales se disputent le contrôle du Royaume des Sept Couronnes sur le continent de Westeros.", true,
   "https://covers.openlibrary.org/b/id/7397050-L.jpg"],
  ["Le Monde de Narnia", "C.S. Lewis", "Fantastique",
   "Quatre enfants découvrent une armoire magique menant au monde enneigé de Narnia gouverné par le lion Aslan.", true,
   "https://covers.openlibrary.org/b/id/10083125-L.jpg"],
  ["L'Histoire sans fin", "Michael Ende", "Fantastique",
   "Bastien lit un livre magique et se retrouve transporté dans le monde de Fantasia pour le sauver du Néant.", true,
   "https://covers.openlibrary.org/b/id/10337889-L.jpg"],
  ["Eragon", "Christopher Paolini", "Fantastique",
   "Un jeune fermier découvre un œuf de dragon bleu et devient le dernier Dragonnier de l'Empire.", true,
   "https://covers.openlibrary.org/b/id/13921600-L.jpg"],
  ["Le Nom du vent", "Patrick Rothfuss", "Fantastique",
   "Kvothe, musicien et magicien légendaire, raconte le récit de sa jeunesse et de son apprentissage.", true,
   "https://covers.openlibrary.org/b/id/11480483-L.jpg"],
  ["La Passe-miroir", "Christelle Dabos", "Fantastique",
   "Ophelia, capable d'animer les objets et traverser les miroirs, est fiancée à un homme de l'Arche du Pôle.", true,
   "https://covers.openlibrary.org/b/id/10232237-L.jpg"],
  ["Shutter Island", "Dennis Lehane", "Policier",
   "Deux US Marshals enquêtent sur la disparition d'une patiente dans un hôpital psychiatrique de haute sécurité sur une île.", false,
   "https://covers.openlibrary.org/b/id/28990-L.jpg"],
];

/** Primary keys that need a sequence attached. Mirrors `installSequences` in
 *  DatabaseConnection.java — see `ensureSequences` below for why both exist. */
const PRIMARY_KEYS = [
  ["livre", "id_livre"],
  ["utilisateur", "id_utilisateur"],
  ["admin", "id_admin"],
  ["lecteur", "id_lecteur"],
  ["emprunt", "id_emprunt"],
  ["reservation", "id_reservation"],
];

/**
 * Gives every primary key a sequence default.
 *
 * The original coursework schema declared plain INTEGER keys and generated ids with
 * `MAX(id) + 1`, which loses writes under concurrency. The desktop client repairs this
 * on connect, but a database the desktop client has never reached still has bare
 * columns with no default — and then *every* insert from the web app fails with a
 * not-null violation on the id. Seeding must not depend on the Java app having been
 * run first, so the same repair happens here.
 *
 * `setval(…, max + 1, false)` makes the next `nextval` return exactly max + 1, so
 * existing rows keep their ids.
 */
async function ensureSequences() {
  for (const [table, column] of PRIMARY_KEYS) {
    const sequence = `${table}_${column}_seq`;
    try {
      await sql.query(`CREATE SEQUENCE IF NOT EXISTS ${sequence}`);
      await sql.query(
        `SELECT setval('${sequence}', COALESCE((SELECT MAX(${column}) FROM ${table}), 0) + 1, false)`,
      );
      await sql.query(
        `ALTER TABLE ${table} ALTER COLUMN ${column} SET DEFAULT nextval('${sequence}')`,
      );
      await sql.query(`ALTER SEQUENCE ${sequence} OWNED BY ${table}.${column}`);
    } catch (error) {
      console.warn(`  could not attach a sequence to ${table}.${column}: ${error.message}`);
    }
  }
}

/** Readers beyond the two demo accounts, so the register shows a range of names. */
const EXTRA_READERS = [
  ["Salma Bennani", "salma.bennani@fsts.ac.ma", 661234501],
  ["Youssef El Amrani", "youssef.elamrani@fsts.ac.ma", 661234502],
  ["Nadia Cherkaoui", "nadia.cherkaoui@fsts.ac.ma", 661234503],
  ["Omar Tazi", "omar.tazi@fsts.ac.ma", 661234504],
];

/**
 * The ten loan situations, as day offsets from the day the seed runs.
 *
 * `returned: null` means the book is still out. Each row is a state the interface
 * renders differently, so the demo exercises every badge and every filter.
 */
const LOAN_SCENARIOS = [
  { label: "Opened today — full loan period remaining", borrowed: 0, due: 14, returned: null },
  { label: "On loan — a week left", borrowed: -7, due: 7, returned: null },
  { label: "Due tomorrow", borrowed: -13, due: 1, returned: null },
  { label: "Due today", borrowed: -14, due: 0, returned: null },
  { label: "Overdue by 3 days", borrowed: -17, due: -3, returned: null },
  { label: "Overdue by 5 weeks — the worst case on the dashboard", borrowed: -49, due: -35, returned: null },
  { label: "Returned a week early", borrowed: -20, due: -6, returned: -13 },
  { label: "Returned exactly on the due date", borrowed: -28, due: -14, returned: -14 },
  { label: "Returned 8 days late", borrowed: -40, due: -26, returned: -18 },
  { label: "Closed historic loan — last term", borrowed: -120, due: -106, returned: -104 },
];

/** Reservation dates, as day offsets. A spread of recent requests. */
const RESERVATION_OFFSETS = [0, -1, -3, -6, -10, -15, -22];

/** `YYYY-MM-DD` for today shifted by `days`, in local time. */
function dayOffset(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

async function main() {
  console.log("Creating schema…");
  // `sql.query` is the plain-string form; the tagged template is for parameterised
  // queries and rejects a bare string.
  for (const statement of TABLES) await sql.query(statement);

  // Migrations are individually optional — most fail simply because they are already
  // applied. Failures are reported rather than swallowed: a silently missing column
  // surfaces much later as a confusing runtime error on the dashboard.
  for (const statement of MIGRATIONS) {
    try {
      await sql.query(statement);
    } catch (error) {
      const alreadyApplied = /already exists|duplicate/i.test(error.message);
      if (!alreadyApplied) {
        console.warn(`  skipped: ${statement.split("\n")[0].trim()}\n    ↳ ${error.message}`);
      }
    }
  }

  console.log("Attaching primary-key sequences…");
  await ensureSequences();

  const [{ count: bookCount }] = await sql`SELECT COUNT(*)::int AS count FROM livre`;
  if (bookCount === 0) {
    console.log(`Seeding ${BOOKS.length} books…`);
    for (const [titre, auteur, genre, resume, dispo, image] of BOOKS) {
      await sql`
        INSERT INTO livre (titre, auteur, genre, resume_livre, disponibilite, image_url)
        VALUES (${titre}, ${auteur}, ${genre}, ${resume}, ${dispo}, ${image ?? null})
      `;
    }
  } else {
    console.log(`Catalogue already has ${bookCount} books — leaving it alone.`);
  }

  const adminId = await ensureAccount("admin", "admin123", "admin@fsts.ac.ma", 612345678, "Admin");
  const lecteurId = await ensureAccount("lecteur", "lecteur123", "lecteur@fsts.ac.ma", 987654321, "Lecteur");

  // Extra readers so the loan register is not a single name repeated ten times.
  const readerIds = [];
  for (const [nom, email, numero] of EXTRA_READERS) {
    readerIds.push(await ensureAccount(nom, "lecteur123", email, numero, "Lecteur"));
  }

  await seedScenarios([lecteurId, ...readerIds], adminId);

  console.log("\nDone. Demo accounts:");
  console.log("  admin   / admin123    (administrator)");
  console.log("  lecteur / lecteur123  (reader)");
  console.log(`  ${EXTRA_READERS.length} further readers, all with password lecteur123`);
}

/**
 * Seeds ten loan situations and a set of reservations.
 *
 * Every date is relative to the day the seed runs, so "overdue by three days" stays
 * overdue whenever the demo is opened rather than drifting into the distant past.
 * The ten cover each state the interface renders differently: on loan, due soon, due
 * today, mildly overdue, badly overdue, returned early, returned on time, returned
 * late, a loan opened today, and a closed historic loan.
 */
async function seedScenarios(readerIds, adminId) {
  const [{ count: loanCount }] = await sql`SELECT COUNT(*)::int AS count FROM emprunt`;

  // On a register that already has rows, adding ten more silently would be rude —
  // those rows may be someone's real testing. Appending is opt-in.
  if (loanCount > 0 && !SEED_SCENARIOS) {
    console.log(
      `Register already has ${loanCount} loan(s) — leaving it alone.\n` +
        "  Add the ten demo scenarios alongside them with: npm run db:seed -- --scenarios",
    );
    return;
  }

  // Only books with no open loan, so a scenario can never double-book a title.
  //
  // The register is the authority here, not `livre.disponibilite`: the flag is
  // denormalised and can be stale — a book left marked available while still out is
  // exactly the case that would otherwise be lent twice.
  const books = await sql`
    SELECT l.id_livre FROM livre l
    WHERE NOT EXISTS (
      SELECT 1 FROM emprunt e
      WHERE e.id_livre = l.id_livre AND e.date_retour_livre IS NULL
    )
    ORDER BY l.id_livre
    LIMIT ${LOAN_SCENARIOS.length}
  `;
  if (books.length < LOAN_SCENARIOS.length) {
    console.warn(`Only ${books.length} available book(s) — seeding that many scenarios.`);
  }

  const total = Math.min(books.length, LOAN_SCENARIOS.length);
  console.log(`\nSeeding ${total} loan scenarios…`);

  for (let i = 0; i < total; i++) {
    const { label, borrowed, due, returned } = LOAN_SCENARIOS[i];
    const bookId = books[i].id_livre;
    const readerId = readerIds[i % readerIds.length];

    await sql`
      INSERT INTO emprunt (id_utilisateur, id_livre, dateEmprunts, dateRetour, date_retour_livre)
      VALUES (
        ${readerId}, ${bookId},
        ${dayOffset(borrowed)}, ${dayOffset(due)},
        ${returned === null ? null : dayOffset(returned)}
      )
    `;

    // A book still out on loan must not show as available.
    await sql`
      UPDATE livre SET disponibilite = ${returned !== null} WHERE id_livre = ${bookId}
    `;
    console.log(`  • ${label}`);
  }

  console.log(`\nSeeding ${RESERVATION_OFFSETS.length + 1} reservations…`);
  for (let i = 0; i < RESERVATION_OFFSETS.length; i++) {
    const readerId = readerIds[i % readerIds.length];
    await sql`
      INSERT INTO reservation (id_utilisateur, dateReservation)
      VALUES (${readerId}, ${dayOffset(RESERVATION_OFFSETS[i])})
    `;
  }

  // One reservation from the administrator, so the list shows both roles.
  if (adminId) {
    await sql`
      INSERT INTO reservation (id_utilisateur, dateReservation)
      VALUES (${adminId}, ${dayOffset(-2)})
    `;
  }
}

/** Creates the account if missing. Returns its id either way. */
async function ensureAccount(nom, password, email, numero, role) {
  const existing = await sql`SELECT id_utilisateur FROM utilisateur WHERE LOWER(nom) = LOWER(${nom})`;
  if (existing.length > 0) {
    console.log(`Account "${nom}" already exists — leaving it alone.`);
    return existing[0].id_utilisateur;
  }

  const stored = await hash(password);
  const rows = await sql`
    INSERT INTO utilisateur (nom, motDePasse, numero, email, role_utilisateur)
    VALUES (${nom}, ${stored}, ${numero}, ${email}, ${role})
    RETURNING id_utilisateur
  `;
  const id = rows[0].id_utilisateur;

  if (role === "Admin") {
    await sql`INSERT INTO admin (id_utilisateur) VALUES (${id})`;
  } else {
    await sql`INSERT INTO lecteur (id_utilisateur) VALUES (${id})`;
  }
  console.log(`Created account "${nom}".`);
  return id;
}

main().catch((error) => {
  console.error("Seed failed:", error.message);
  process.exit(1);
});
