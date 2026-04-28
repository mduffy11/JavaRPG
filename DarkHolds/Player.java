import java.util.ArrayList;

public class Player {
    private static final int[] XP_THRESHOLDS = {0, 12, 26, 42, 60, 82, 108, 138, 172, 210, 252, 298, 348};
    private static final int[] HP_BY_LEVEL = {50, 54, 58, 62, 66, 70, 74, 79, 84, 88, 92, 96, 100};
    private static final int[] ATTACK_BY_LEVEL = {8, 8, 9, 9, 10, 10, 11, 12, 12, 13, 14, 15, 16};
    private static final int[] DEFENSE_BY_LEVEL = {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6};

    private final String name;
    private int hp;
    private int maxHp;
    private int baseAttack;
    private int baseDefense;
    private int level;
    private int xp;
    private int gold;
    private String currentRoom;
    private String previousRoom;
    private final ArrayList<Item> inventory;
    private final ArrayList<KeyItem> keys;

    public Player(String name) {
        this.name = name;
        this.level = 1;
        this.xp = 0;
        this.gold = 0;
        this.maxHp = HP_BY_LEVEL[0];
        this.hp = maxHp;
        this.baseAttack = ATTACK_BY_LEVEL[0];
        this.baseDefense = DEFENSE_BY_LEVEL[0];
        this.inventory = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.inventory.add(Items.stick());
    }

    public void takeDamage(int amount) { hp = Math.max(0, hp - amount); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public void increaseBaseAttack(int amount) { this.baseAttack += amount; }
    public boolean isAlive() { return hp > 0; }
    public void addXp(int amount) { xp += amount; }

    public void checkLevelUp() {
        while (level < HP_BY_LEVEL.length && xp >= XP_THRESHOLDS[level]) {
            level++;
            maxHp = HP_BY_LEVEL[level - 1];
            baseAttack = ATTACK_BY_LEVEL[level - 1];
            baseDefense = DEFENSE_BY_LEVEL[level - 1];
            hp = maxHp;
            System.out.println("You reached level " + level + "!\nYour strength rises.");
        }
    }

    public void addGold(int amount) { gold += amount; }
    public void addItem(Item item) { inventory.add(item); }
    public void removeItem(int index) { inventory.remove(index); }
    
    public boolean hasItem(String itemName) {
        for (Item item : inventory) { if (item.getName().equalsIgnoreCase(itemName)) return true; }
        return false;
    }

    public void addKey(KeyItem key) { keys.add(key); }
    public boolean hasAnyKey() { return !keys.isEmpty(); }
    public boolean hasKey(String keyId) {
        for (KeyItem key : keys) { if (key.getKeyId().equalsIgnoreCase(keyId)) return true; }
        return false;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return baseAttack + Items.getPassiveAttackBonus(this); }
    public int getDefense() { return baseDefense + Items.getPassiveDefenseBonus(this); }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getGold() { return gold; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }
    public String getPreviousRoom() { return previousRoom; }
    public void setPreviousRoom(String previousRoom) { this.previousRoom = previousRoom; }
    public ArrayList<Item> getInventory() { return inventory; }
    public ArrayList<KeyItem> getKeys() { return keys; }
}