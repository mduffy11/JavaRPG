import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public final class Bestiary {
    private static final Random random = new Random();

    private Bestiary() {}

    public static Enemy bat() {
        return new Enemy(EnemyType.BAT, "Bats", 14, 4, 1, 7, randomGold(1, 4),
                new EnemyMove("Bite", 1, 84, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wing Buffet", 2, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Screech", 1, 78, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy goblin() {
        return new Enemy(EnemyType.GOBLIN, "Goblin", 20, 5, 2, 10, randomGold(4, 8),
                new EnemyMove("Knife Stab", 2, 80, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wild Swing", 4, 58, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pocket Sand", 0, 52, MoveType.STATUS, false, MoveEffect.POCKET_SAND, 50, 0));
    }

    public static Enemy wolf() {
        return new Enemy(EnemyType.WOLF, "Wolf", 24, 6, 2, 13, randomGold(4, 7),
                new EnemyMove("Snap Bite", 2, 82, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Lunge", 4, 68, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pounce", 5, 56, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0));
    }

    public static Enemy slime() {
        return new Enemy(EnemyType.SLIME, "Slime", 42, 4, 5, 20, randomGold(8, 15),
                new EnemyMove("Slam", 2, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Body Press", 4, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Acid Splash", 3, 70, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy skeleton() {
        return new Enemy(EnemyType.SKELETON, "Skeleton", 30, 7, 4, 17, randomGold(6, 10),
                new EnemyMove("Rusty Slash", 3, 78, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Shield Bash", 5, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Bone Rattle", 1, 74, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy orc() {
        return new Enemy(EnemyType.ORC, "Orc", 34, 8, 3, 19, randomGold(8, 14),
                new EnemyMove("Axe Chop", 4, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Cleaver Drop", 6, 58, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("War Cry", 2, 80, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy ogre() {
        return new Enemy(EnemyType.OGRE, "Ogre", 52, 11, 4, 30, randomGold(12, 20),
                new EnemyMove("Club Smash", 7, 60, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Backhand", 4, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Ground Stomp", 3, 82, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy caveTroll() {
        return new Enemy(EnemyType.CAVE_TROLL, "Cave Troll", 64, 12, 6, 36, randomGold(15, 24),
                new EnemyMove("Maul", 7, 66, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Crushing Grab", 5, 62, MoveType.GRAPPLE, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Regenerate", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 8));
    }

    public static Enemy giantSnake() {
        return new Enemy(EnemyType.SNAKE, "Giant Snake", 34, 8, 3, 42, randomGold(15, 25),
                new EnemyMove("Bite", 3, 78, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tail Lash", 5, 54, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Constrict", 2, 60, MoveType.GRAPPLE, false, MoveEffect.CONSTRICT, 100, 0));
    }

    public static Enemy necromancer() {
        return new Enemy(EnemyType.NECROMANCER, "Necromancer", 86, 13, 7, 80, randomGold(40, 60),
                new EnemyMove("Bone Spear", 7, 84, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Grave Lash", 5, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Soul Drain", 5, 72, MoveType.MAGIC, false, MoveEffect.SOUL_DRAIN, 100, 5),
                new EnemyMove("Raise Dead", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 10),
                new EnemyMove("Black Flame", 8, 64, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0));
    }

    private static int randomGold(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

class Enemy {
    private final EnemyType type;
    private final String name;
    private final int maxHp, attack, defense, xpReward, goldReward;
    private final ArrayList<EnemyMove> moves;
    private int hp;
    private boolean defeated;

    public Enemy(EnemyType type, String name, int hp, int attack, int defense, int xpReward, int goldReward, EnemyMove... moves) {
        this.type = type; this.name = name; this.maxHp = hp; this.hp = hp;
        this.attack = attack; this.defense = defense; this.xpReward = xpReward; this.goldReward = goldReward;
        this.defeated = false;
        this.moves = new ArrayList<>();
        Collections.addAll(this.moves, moves);
    }

    public EnemyMove chooseMove(Random random) { return moves.get(random.nextInt(moves.size())); }
    public void takeDamage(int amount) { hp = Math.max(0, hp - amount); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public boolean isAlive() { return hp > 0; }

    public EnemyType getType() { return type; }
    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getXpReward() { return xpReward; }
    public int getGoldReward() { return goldReward; }
    public boolean isDefeated() { return defeated; }
    public void setDefeated(boolean defeated) { this.defeated = defeated; }
}

class EnemyMove {
    private final String name;
    private final int power, accuracy, effectChance, effectPower;
    private final MoveType moveType;
    private final boolean parryable;
    private final MoveEffect effect;

    public EnemyMove(String name, int power, int accuracy, MoveType moveType, boolean parryable, MoveEffect effect, int effectChance, int effectPower) {
        this.name = name; this.power = power; this.accuracy = accuracy;
        this.moveType = moveType; this.parryable = parryable;
        this.effect = effect; this.effectChance = effectChance; this.effectPower = effectPower;
    }

    public String getName() { return name; }
    public int getPower() { return power; }
    public int getAccuracy() { return accuracy; }
    public MoveType getMoveType() { return moveType; }
    public boolean isParryable() { return parryable; }
    public MoveEffect getEffect() { return effect; }
    public int getEffectChance() { return effectChance; }
    public int getEffectPower() { return effectPower; }
}

enum EnemyType { BAT, GOBLIN, WOLF, SLIME, SKELETON, ORC, OGRE, CAVE_TROLL, SNAKE, NECROMANCER }
enum MoveType { LIGHT, NORMAL, HEAVY, GRAPPLE, STATUS, MAGIC }
enum MoveEffect { NONE, POCKET_SAND, CONSTRICT, SELF_HEAL, SOUL_DRAIN }