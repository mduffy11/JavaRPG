import java.util.Random;

final class DarkHoldsBestiary {
    private static final Random random = new Random();

    private DarkHoldsBestiary() {
    }

    /* =====================================================
       TIER 1 ENEMIES
       ===================================================== */

    public static Enemy bat() {
        return new Enemy(
                EnemyType.BAT,
                "Bats",
                14,
                4,
                1,
                7,
                randomGold(1, 4),
                new EnemyMove("Bite", 1, 84, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wing Buffet", 2, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Screech", 1, 78, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy goblin() {
        return new Enemy(
                EnemyType.GOBLIN,
                "Goblin",
                20,
                5,
                2,
                10,
                randomGold(4, 8),
                new EnemyMove("Knife Stab", 2, 80, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wild Swing", 4, 58, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pocket Sand", 0, 52, MoveType.STATUS, false, MoveEffect.POCKET_SAND, 50, 0)
        );
    }

    public static Enemy wolf() {
        return new Enemy(
                EnemyType.WOLF,
                "Wolf",
                24,
                6,
                2,
                13,
                randomGold(4, 7),
                new EnemyMove("Snap Bite", 2, 82, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Lunge", 4, 68, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pounce", 5, 56, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0)
        );
    }

    /* =====================================================
       TIER 2 ENEMIES
       ===================================================== */

    public static Enemy slime() {
        return new Enemy(
                EnemyType.SLIME,
                "Slime",
                42,
                4,
                5,
                20,
                randomGold(8, 15),
                new EnemyMove("Slam", 2, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Body Press", 4, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Acid Splash", 3, 70, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy skeleton() {
        return new Enemy(
                EnemyType.SKELETON,
                "Skeleton",
                30,
                7,
                4,
                17,
                randomGold(6, 10),
                new EnemyMove("Rusty Slash", 3, 78, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Shield Bash", 5, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Bone Rattle", 1, 74, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy orc() {
        return new Enemy(
                EnemyType.ORC,
                "Orc",
                34,
                8,
                3,
                19,
                randomGold(8, 14),
                new EnemyMove("Axe Chop", 4, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Cleaver Drop", 6, 58, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("War Cry", 2, 80, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    /* =====================================================
       TIER 3 ENEMIES
       ===================================================== */

    public static Enemy ogre() {
        return new Enemy(
                EnemyType.OGRE,
                "Ogre",
                52,
                11,
                4,
                30,
                randomGold(12, 20),
                new EnemyMove("Club Smash", 7, 60, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Backhand", 4, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Ground Stomp", 3, 82, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy caveTroll() {
        return new Enemy(
                EnemyType.CAVE_TROLL,
                "Cave Troll",
                64,
                12,
                6,
                36,
                randomGold(15, 24),
                new EnemyMove("Maul", 7, 66, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Crushing Grab", 5, 62, MoveType.GRAPPLE, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Regenerate", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 8)
        );
    }

    /* =====================================================
       BOSSES
       ===================================================== */

    public static Enemy giantSnake() {
        return new Enemy(
                EnemyType.SNAKE,
                "Giant Snake",
                34,
                8,
                3,
                42,
                randomGold(15, 25),
                new EnemyMove("Bite", 3, 78, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tail Lash", 5, 54, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Constrict", 2, 60, MoveType.GRAPPLE, false, MoveEffect.CONSTRICT, 100, 0)
        );
    }

    public static Enemy necromancer() {
        return new Enemy(
                EnemyType.NECROMANCER,
                "Necromancer",
                86,
                13,
                7,
                80,
                randomGold(40, 60),
                new EnemyMove("Bone Spear", 7, 84, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Grave Lash", 5, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Soul Drain", 5, 72, MoveType.MAGIC, false, MoveEffect.SOUL_DRAIN, 100, 5),
                new EnemyMove("Raise Dead", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 10),
                new EnemyMove("Black Flame", 8, 64, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0)
        );
    }

    private static int randomGold(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
