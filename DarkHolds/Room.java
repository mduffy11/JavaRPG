import java.util.ArrayList;

public class Room {
    private final String id;
    private final String name;
    private final String description;
    private String clearedDescription;
    private final ArrayList<String> neighborIds;
    private final ArrayList<Item> rewardItems;
    private Enemy enemy;
    private int goldReward;
    private boolean hasKeyReward;
    private boolean rewardsCollected;
    private boolean preLock;
    private boolean keyEligible;
    private final boolean puzzleRoom;
    private boolean puzzleSolved;

    public Room(String id, String name, String description, boolean preLock, boolean keyEligible) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.clearedDescription = "This room is still and quiet now.";
        this.preLock = preLock;
        this.keyEligible = keyEligible;
        this.neighborIds = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
        this.goldReward = 0;
        this.hasKeyReward = false;
        this.rewardsCollected = false;
        this.puzzleRoom = id.contains("p_");
        this.puzzleSolved = false;
    }

    public void addNeighbor(String neighborId) { if (!neighborIds.contains(neighborId)) neighborIds.add(neighborId); }
    public void addRewardItem(Item item) { rewardItems.add(item); }
    public void addGold(int amount) { goldReward += amount; }
    public boolean hasEnemy() { return enemy != null; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayDescription() { return hasBeenCleared() ? clearedDescription : description; }
    public void setClearedDescription(String clearedDescription) { this.clearedDescription = clearedDescription; }

    public boolean hasBeenCleared() {
        if (hasEnemy() && enemy.isDefeated()) return true;
        if (isPuzzleRoom() && puzzleSolved) return true;
        return rewardsCollected;
    }

    public ArrayList<String> getNeighborIds() { return neighborIds; }
    public Enemy getEnemy() { return enemy; }
    public void setEnemy(Enemy enemy) { this.enemy = enemy; }
    public int getGoldReward() { return goldReward; }
    public boolean hasKeyReward() { return hasKeyReward; }
    public void setHasKeyReward(boolean hasKeyReward) { this.hasKeyReward = hasKeyReward; }
    public ArrayList<Item> getRewardItems() { return rewardItems; }
    public boolean isRewardsCollected() { return rewardsCollected; }
    public void setRewardsCollected(boolean rewardsCollected) { this.rewardsCollected = rewardsCollected; }
    public boolean isPreLock() { return preLock; }
    public void setPreLock(boolean preLock) { this.preLock = preLock; }
    public boolean isKeyEligible() { return keyEligible; }
    public void setKeyEligible(boolean keyEligible) { this.keyEligible = keyEligible; }
    public boolean isPuzzleRoom() { return puzzleRoom; }
    public boolean isPuzzleSolved() { return puzzleSolved; }
    public void setPuzzleSolved(boolean puzzleSolved) { this.puzzleSolved = puzzleSolved; }
}