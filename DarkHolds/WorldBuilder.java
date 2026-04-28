import java.util.*;

public class WorldBuilder {

    public static List<Room> createFloorOne(Random random) {
        List<Room> f1Rooms = new ArrayList<>();
        int numRooms = random.nextInt(7) + 7; 

        Room safeRoom = new Room("f1safe", "Safe Room", "You wake in a cold stone chamber with only a rough stick beside you. A heavy locked hallway gate stands to one side, and the rest of the floor branches into darkness.", true, true);

        List<Room> f1Pool = new ArrayList<>();
        f1Pool.add(new Room("f1p_brokenFork", "Broken Fork", "The passage splits here around cracked masonry. An ancient puzzle locks the path.", true, true));
        f1Pool.add(new Room("f1p_shrine", "Serpent Shrine", "A shrine of coiled stone and old offerings. A riddle is carved into the altar.", true, true));
        f1Pool.add(new Room("f1p_weighingScales", "Room of Scales", "A massive bronze scale dominates the center of the room. One side is weighed down by a stone heart; the other side sits empty, waiting for an offering.", true, true));
        
        Room batRoom = new Room("f1f_batRoost", "Bat Roost", "The ceiling above is black with restless shapes. Thin squeaks echo between the stones.", true, true);
        batRoom.setEnemy(Bestiary.bat());
        f1Pool.add(batRoom);

        Room goblinRoom = new Room("f1f_goblinOne", "Goblin Trail", "The smell of rot and old smoke thickens. You spot a goblin scavenger lurking among pilfered junk.", true, true);
        goblinRoom.setEnemy(Bestiary.goblin());
        f1Pool.add(goblinRoom);

        Room slimeRoom = new Room("f1f_slimeRoom", "Damp Tunnel", "A wet side cut opens into a slick pocket where something gelatinous shivers in the dark.", true, true);
        slimeRoom.setEnemy(Bestiary.slime());
        f1Pool.add(slimeRoom);
        
        Room chimneyShaft = new Room("f1f_chimneyShaft", "Crumbling Chimney", "A vertical shaft lets in a sliver of moonlight. A swarm of bats uses this as a highway to the surface.", true, true);
        chimneyShaft.setEnemy(Bestiary.bat());
        f1Pool.add(chimneyShaft);

        Room batRoots = new Room("f1f_bat_roots", "Hanging Roots", "Thick, dead tree roots punch through the ceiling here. Dozens of sleeping bats cling to them.", true, true);
        batRoots.setEnemy(Bestiary.bat());
        f1Pool.add(batRoots);

        Room wolfDen = new Room("f1f_wolf_den", "Beast's Den", "The floor is covered in matted fur. A massive wolf blocks the exit, growling.", true, true);
        wolfDen.setEnemy(Bestiary.wolf());
        f1Pool.add(wolfDen);

        Room wolfTunnel = new Room("f1f_wolf_tunnel", "Howling Tunnel", "A real howl answers the draft as a wolf lunges from the shadows.", true, true);
        wolfTunnel.setEnemy(Bestiary.wolf());
        f1Pool.add(wolfTunnel);

        f1Pool.add(new Room("f1_cache", "Empty Cache", "A looted side chamber lies here with only scattered coins left behind.", true, true));
        f1Pool.add(new Room("f1_snakeHoard", "Snake Hoard", "Gold glints among bones and torn packs.", true, true));
        f1Pool.add(new Room("f1_fallenAdventurer", "Fallen Adventurer", "A skeleton slumps against the wall, clutching a broken weapon.", true, true));

        Collections.shuffle(f1Pool, random);
        f1Rooms.add(safeRoom);
        f1Rooms.addAll(f1Pool.subList(0, numRooms - 4)); 

        Room lockedHall = new Room("f1lockedHall", "Locked Hallway", "With the key in hand, you force the rusted mechanism open.", false, false);
        f1Rooms.add(lockedHall);

        Room f1Boss = new Room("f1snakeBoss", "Serpent Lair", "A giant snake rises from the treasure mound, its eyes fixed on you.", false, false);
        f1Boss.setEnemy(Bestiary.giantSnake());
        f1Rooms.add(f1Boss);

        Room stairs1 = new Room("f1_stairs", "Stairs to Floor 2", "A spiral stone staircase leading down into the darkness.", false, false);
        f1Rooms.add(stairs1);

        for (int i = 0; i < f1Rooms.size() - 2; i++) connect(f1Rooms.get(i), f1Rooms.get(i + 1));
        f1Rooms.get(f1Rooms.size() - 2).addNeighbor(f1Rooms.get(f1Rooms.size() - 1).getId());

        return f1Rooms;
    }

    public static List<Room> createFloorTwo(Random random) {
        List<Room> f2Rooms = new ArrayList<>();
        int numRooms = random.nextInt(8) + 8; 

        List<Room> f2Pool = new ArrayList<>();
        f2Pool.add(new Room("f2p_totem", "Totem of Gruum", "A crude wooden totem pole stands before a locked portcullis.", false, false));
        f2Pool.add(new Room("f2p_weepingstatue", "Weeping Statue", "A towering marble statue of a hooded figure weeps oily water.", false, false));
        f2Pool.add(new Room("f2p_campsite", "Abandoned Campsite", "A dusty bedroll and a cold firepit sit in the corner.", false, false));
        f2Pool.add(new Room("f2p_wishwell", "The Wishing Well", "A deep stone well sits in the center of the room.", false, false));
        f2Pool.add(new Room("f2p_obsidian_obelisk", "Obsidian Obelisk", "A perfectly smooth, black stone spire stands in the center.", false, false));

        Room altar = new Room("f2_shamansAltar", "The Shaman's Altar", "A makeshift altar of stacked stones is covered in melted wax.", false, false);
        altar.addRewardItem(Items.bigPotion());
        f2Pool.add(altar);

        Room armory = new Room("f2_desecratedArmory", "Desecrated Armory", "Shattered shields and bent swords litter the floor.", false, false);
        if (random.nextBoolean()) armory.addRewardItem(Items.smallPotion());
        else armory.addGold(randomGold(7, 20, random));
        f2Pool.add(armory);

        Room slimePool = new Room("f2f_slime_pool", "Acidic Pool", "A bubbling puddle rises upward, forming into a massive blob.", false, false);
        slimePool.setEnemy(Bestiary.slime());
        f2Pool.add(slimePool);

        Room slimeCeiling = new Room("f2f_slime_ceiling", "The Dripping Ceiling", "The ceiling is coated in a vibrating slime that suddenly drops.", false, false);
        slimeCeiling.setEnemy(Bestiary.slime());
        f2Pool.add(slimeCeiling);

        Room skelCrypt = new Room("f2f_skel_crypt", "Forgotten Crypt", "A skeleton steps out of a sarcophagus, raising a rusted sword.", false, false);
        skelCrypt.setEnemy(Bestiary.skeleton());
        f2Pool.add(skelCrypt);

        Room skelGrave = new Room("f2f_skel_grave", "The Mass Grave", "A skeletal hand bursts from the dirt, followed by the warrior.", false, false);
        skelGrave.setEnemy(Bestiary.skeleton());
        f2Pool.add(skelGrave);

        Room fightingPit = new Room("f2f_fighting_pit", "The Fighting Pit", "A battle-crazed Orc leaps down from the spectator ledge.", false, false);
        fightingPit.setEnemy(Bestiary.orc());
        f2Pool.add(fightingPit);

        Room raidersBarricade = new Room("f2f_raiders_barricade", "Raider's Barricade", "A scarred Orc barks an alarm as you approach the chokepoint.", false, false);
        raidersBarricade.setEnemy(Bestiary.orc());
        f2Pool.add(raidersBarricade);

        Room alchemistSpill = new Room("f2f_alchemist_spill", "Alchemist's Spill", "Spilled alchemical reagents have coalesced into a bubbling Slime.", false, false);
        alchemistSpill.setEnemy(Bestiary.slime());
        f2Pool.add(alchemistSpill);

        Room spoilsRoom = new Room("f2f_spoils_room", "Spoils Room", "An Orc is busy biting coins to check their worth.", false, false);
        spoilsRoom.setEnemy(Bestiary.orc());
        f2Pool.add(spoilsRoom);

        Collections.shuffle(f2Pool, random);
        f2Rooms.addAll(f2Pool.subList(0, numRooms - 2)); 

        Room golemBoss = new Room("f2_golem_rune", "Runestone Chamber", "A Golem made entirely of carved runestones stands guard.", false, false);
        golemBoss.setEnemy(Bestiary.bat()); 
        f2Rooms.add(golemBoss);

        Room stairs2 = new Room("f2_stairs", "Stairs to Floor 3", "A deep, echoing stairwell descends further into the dark holds.", false, false);
        f2Rooms.add(stairs2);

        for (int i = 0; i < f2Rooms.size() - 2; i++) connect(f2Rooms.get(i), f2Rooms.get(i + 1));
        f2Rooms.get(f2Rooms.size() - 2).addNeighbor(f2Rooms.get(f2Rooms.size() - 1).getId());

        return f2Rooms;
    }

    public static List<Room> createFloorThree(Random random) {
        List<Room> f3Rooms = new ArrayList<>();
        int numRooms = random.nextInt(9) + 9; 
        
        List<Room> f3Pool = new ArrayList<>();
        f3Pool.add(new Room("f3p_silentlibrary", "Silent Library", "Rows of rotting bookshelves line this chamber.", false, false));
        f3Pool.add(new Room("f3p_colorpillars", "Color Pillars", "Three pillars stand before a locked door. Red, blue, and yellow.", false, false));
        f3Pool.add(new Room("f3p_hourglass", "Hourglass of Sand", "A giant hourglass flips when you enter.", false, false));
        f3Pool.add(new Room("f3p_luckschance", "Luck's Chance", "You encounter a room with three chests.", false, false));
        f3Pool.add(new Room("f3p_lakeoftruth", "Lake of Truth", "Two daggers rise from a pool. One silver, one gold.", false, false));
        f3Pool.add(new Room("f3p_starmap_room", "Star Map Room", "The ceiling is painted with a glowing map of the night sky.", false, false));
        f3Pool.add(new Room("f3p_crimson_altar", "The Crimson Altar", "A pristine white marble altar sits in the center.", false, false));
        f3Pool.add(new Room("f3p_abandoned_lab", "Abandoned Laboratory", "A central cauldron bubbles with a sweet-smelling pink liquid.", false, false));
        f3Pool.add(new Room("f3_guardhouse", "Abandoned Guardhouse", "Weapon racks stand empty.", false, false));
        f3Pool.add(new Room("f3_statueGallery", "Statue Gallery", "A hall lined with statues of faceless knights.", false, false));
        f3Pool.add(new Room("f3_armorers_forge", "Armorer's Forge", "An unfinished breastplate rests on a table.", false, false));

        Room bonePit = new Room("f3f_bonePit", "The Bone Pit", "A hulking Ogre slowly turns towards you in a pit of bones.", false, false);
        bonePit.setEnemy(Bestiary.ogre()); 
        f3Pool.add(bonePit);

        Room tremblingCavern = new Room("f3f_tremblingCavern", "The Trembling Cavern", "An Ogre wearing scavenged armor plates beats its chest and charges.", false, false);
        tremblingCavern.setEnemy(Bestiary.ogre());
        f3Pool.add(tremblingCavern);

        Room trollsHoard = new Room("f3f_trollsHoard", "Troll's Hoard", "The Troll protecting a pile of shiny trash roars fiercely.", false, false);
        trollsHoard.setEnemy(Bestiary.caveTroll());
        f3Pool.add(trollsHoard);

        Room echoingChasm = new Room("f3f_echoingChasm", "Echoing Chasm", "A Cave Troll hangs from the stalactites above.", false, false);
        echoingChasm.setEnemy(Bestiary.caveTroll());
        f3Pool.add(echoingChasm);

        Room cryptKings = new Room("f3_crypt_kings", "Crypt of the Forgotten Kings", "Ornate sarcophagi rest in alcoves.", false, false);
        cryptKings.addGold(randomGold(15, 25, random));
        f3Pool.add(cryptKings);

        Collections.shuffle(f3Pool, random);
        f3Rooms.addAll(f3Pool.subList(0, numRooms - 2)); 

        Room necroBoss = new Room("f3_necro_altar", "Profane Altar", "A Necromancer in tattered robes is chanting.", false, false);
        necroBoss.setEnemy(Bestiary.necromancer());
        f3Rooms.add(necroBoss);

        Room f3End = new Room("f3_end", "The Final Descent", "You step past the profane altar, having conquered the prototype...", false, false);
        f3Rooms.add(f3End);

        for (int i = 0; i < f3Rooms.size() - 2; i++) connect(f3Rooms.get(i), f3Rooms.get(i + 1));
        f3Rooms.get(f3Rooms.size() - 2).addNeighbor(f3Rooms.get(f3Rooms.size() - 1).getId());

        return f3Rooms;
    }

    public static void connect(Room a, Room b) {
        a.addNeighbor(b.getId());
        b.addNeighbor(a.getId());
    }
    
    private static int randomGold(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }
}