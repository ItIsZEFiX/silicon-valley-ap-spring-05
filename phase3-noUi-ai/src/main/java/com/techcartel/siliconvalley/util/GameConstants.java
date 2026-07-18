package com.techcartel.siliconvalley.util;

/**
 * Central place for every constant / magic number used across the game.
 * Keeping them here (instead of scattered literals) is a hard requirement
 * of the project's "code quality" grading section.
 */
public final class GameConstants {

    private GameConstants() {
        // utility class, no instances
    }

    // ---- Map ----
    public static final int GRID_SIZE = 5;      // 5x5 sectors
    public static final int VERTEX_SIZE = 6;    // 6x6 vertices
    public static final int MIN_VERTEX_DISTANCE = 2; // min edges between two companies

    // ---- Players ----
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    public static final int WINNING_SCORE = 10;

    // ---- Dice ----
    public static final int DIE_MIN = 1;
    public static final int DIE_MAX = 6;
    public static final int CRISIS_ROLL = 7;

    // ---- Market ----
    public static final int BASE_PRICE = 4;
    public static final int MAX_PRICE = 6;
    public static final int MIN_PRICE = 2;
    public static final int ROUNDS_BEFORE_PRICE_DROP = 3;

    // ---- Structure costs ----
    // MVP: 1 Capital, 1 Talent, 1 Cloud, 1 Data
    public static final int MVP_CAPITAL_COST = 1;
    public static final int MVP_TALENT_COST = 1;
    public static final int MVP_CLOUD_COST = 1;
    public static final int MVP_DATA_COST = 1;

    // Unicorn upgrade: 2 Cloud, 3 Data (Tech Guru CTO pays only 1 Cloud)
    public static final int UNICORN_CLOUD_COST = 2;
    public static final int UNICORN_CLOUD_COST_TECH_GURU = 1;
    public static final int UNICORN_DATA_COST = 3;

    // Partnership: 1 Capital, 1 Patent
    public static final int PARTNERSHIP_CAPITAL_COST = 1;
    public static final int PARTNERSHIP_PATENT_COST = 1;

    // ---- Founder roles ----
    public static final int ROLE_VICTORY_POINT_PENALTY = 1;
    public static final int VC_FUNDED_STARTING_CAPITAL = 2;
    public static final int STANDARD_CRISIS_CARD_LIMIT = 7;
    public static final int VC_FUNDED_CRISIS_CARD_LIMIT = 9;
    public static final int HACKER_CEO_TRADE_DISCOUNT = 1; // pays 1 less capital per market buy

    // ---- Scoring ----
    public static final int MVP_VICTORY_POINTS = 1;
    public static final int UNICORN_VICTORY_POINTS = 2;
    public static final int LONGEST_NETWORK_MIN_LENGTH = 3;
    public static final int LONGEST_NETWORK_BONUS = 2;

    // ---- Resource generation ----
    public static final int MVP_PRODUCTION = 1;
    public static final int UNICORN_PRODUCTION = 2;

    // ---- Setup phase ----
    public static final int SETUP_ROUNDS = 2;
}
