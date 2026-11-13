package battleship.ui;

import java.awt.Color;
import java.awt.Font;

/** Centralized palette, typography, and spacing for the naval chart interface. */
public final class UiTokens {
    private UiTokens() { }

    // Primitive palette
    public static final Color NAVY_CANVAS = new Color(0x0B1320);
    public static final Color SLATE_SURFACE = new Color(0x121D2F);
    public static final Color SLATE_ELEVATED = new Color(0x19263D);
    public static final Color GRID_HAIRLINE = new Color(0x263854);
    public static final Color OCEAN_BASE = new Color(0x0F243B);
    public static final Color OCEAN_HOVER = new Color(0x163354);
    public static final Color STEEL_SHIP = new Color(0x64748B);
    public static final Color STEEL_EDGE = new Color(0x94A3B8);
    public static final Color HIT_CRIMSON = new Color(0xDC2626);
    public static final Color HIT_MARK = new Color(0xFEE2E2);
    public static final Color MISS_WATER = new Color(0x1E3A5F);
    public static final Color SIGNAL_AMBER = new Color(0xD97706);
    public static final Color TEXT_PRIMARY_COLOR = new Color(0xF8FAFC);
    public static final Color TEXT_SECONDARY_COLOR = new Color(0x94A3B8);
    public static final Color PREVIEW_VALID_COLOR = new Color(0x22C55E);
    public static final Color PREVIEW_INVALID_COLOR = new Color(0xF97316);
    public static final Color BTN_PRIMARY_BG_COLOR = new Color(0xD97706);
    public static final Color BTN_PRIMARY_HOVER_COLOR = new Color(0xF59E0B);
    public static final Color BTN_PRIMARY_PRESSED_COLOR = new Color(0xB45309);
    public static final Color BTN_PRIMARY_FG_COLOR = new Color(0x0B1320);
    public static final Color BTN_SECONDARY_BG_COLOR = new Color(0x19263D);
    public static final Color BTN_SECONDARY_HOVER_COLOR = new Color(0x263854);
    public static final Color BTN_DISABLED_BG_COLOR = new Color(0x263244);
    public static final Color BTN_DISABLED_FG_COLOR = new Color(0x64748B);
    public static final Color STATUS_SUCCESS_COLOR = new Color(0x4ADE80);
    public static final Color STATUS_WARNING_COLOR = new Color(0xFBBF24);
    public static final Color STATUS_DANGER_COLOR = new Color(0xF87171);
    public static final Color MAGIC_FIRE_COLOR = new Color(0xF97316);
    public static final Color MAGIC_LIGHTNING_COLOR = new Color(0xFACC15);
    public static final Color MAGIC_SONAR_COLOR = new Color(0x14B8A6);
    public static final Color MAGIC_FROST_COLOR = new Color(0x38BDF8);
    public static final Color MANA_FILL_COLOR = new Color(0x38BDF8);
    public static final Color MANA_EMPTY_COLOR = new Color(0x1E293B);
    public static final Color REVEALED_TINT_COLOR = new Color(0x14B8A6);
    public static final Color SHIELD_TINT_COLOR = new Color(0x38BDF8);

    // Semantic palette
    public static final Color BG_CANVAS = NAVY_CANVAS;
    public static final Color BG_SURFACE = SLATE_SURFACE;
    public static final Color BG_ELEVATED = SLATE_ELEVATED;
    public static final Color BG_BOARD_EMPTY = OCEAN_BASE;
    public static final Color BG_BOARD_HOVER = OCEAN_HOVER;
    public static final Color BORDER_SUBTLE = GRID_HAIRLINE;
    public static final Color TEXT_PRIMARY = TEXT_PRIMARY_COLOR;
    public static final Color TEXT_MUTED = TEXT_SECONDARY_COLOR;
    public static final Color STATE_SHIP = STEEL_SHIP;
    public static final Color STATE_HIT = HIT_CRIMSON;
    public static final Color STATE_MISS = MISS_WATER;
    public static final Color ACCENT_SIGNAL = SIGNAL_AMBER;
    public static final Color FOCUS_RING = HIT_MARK;
    public static final Color PREVIEW_VALID = PREVIEW_VALID_COLOR;
    public static final Color PREVIEW_INVALID = PREVIEW_INVALID_COLOR;
    public static final Color BTN_PRIMARY_BG = BTN_PRIMARY_BG_COLOR;
    public static final Color BTN_PRIMARY_HOVER = BTN_PRIMARY_HOVER_COLOR;
    public static final Color BTN_PRIMARY_PRESSED = BTN_PRIMARY_PRESSED_COLOR;
    public static final Color BTN_PRIMARY_FG = BTN_PRIMARY_FG_COLOR;
    public static final Color BTN_SECONDARY_BG = BTN_SECONDARY_BG_COLOR;
    public static final Color BTN_SECONDARY_HOVER = BTN_SECONDARY_HOVER_COLOR;
    public static final Color BTN_DISABLED_BG = BTN_DISABLED_BG_COLOR;
    public static final Color BTN_DISABLED_FG = BTN_DISABLED_FG_COLOR;
    public static final Color STATUS_SUCCESS = STATUS_SUCCESS_COLOR;
    public static final Color STATUS_WARNING = STATUS_WARNING_COLOR;
    public static final Color STATUS_DANGER = STATUS_DANGER_COLOR;
    public static final Color MAGIC_FIRE = MAGIC_FIRE_COLOR;
    public static final Color MAGIC_LIGHTNING = MAGIC_LIGHTNING_COLOR;
    public static final Color MAGIC_SONAR = MAGIC_SONAR_COLOR;
    public static final Color MAGIC_FROST = MAGIC_FROST_COLOR;
    public static final Color MANA_FILL = MANA_FILL_COLOR;
    public static final Color MANA_EMPTY = MANA_EMPTY_COLOR;
    public static final Color REVEALED_TINT = REVEALED_TINT_COLOR;
    public static final Color SHIELD_TINT = SHIELD_TINT_COLOR;

    // Component rhythm and sizing
    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 16;
    public static final int SPACE_LG = 24;
    public static final int CELL_SIZE_DEFAULT = 40;
    public static final int HEADER_THICKNESS = 28;
    public static final int RADIUS_CARD = 10;
    public static final int HEADER_HEIGHT = 56;
    public static final int FRAME_WIDTH = 1160;
    public static final int FRAME_HEIGHT = 900;
    public static final int FRAME_MIN_WIDTH = 980;
    public static final int FRAME_MIN_HEIGHT = 840;
    public static final int BOARD_LABEL_GUTTER = 32;
    public static final int BOARD_MIN_CELL_SIZE = 20;
    public static final int BOARD_PREFERRED_WIDTH = 500;
    public static final int BOARD_PREFERRED_HEIGHT = 468;
    public static final int LEGEND_SWATCH_SIZE = 24;
    public static final int LOGO_SIZE = 32;
    public static final int CONTROL_PANEL_WIDTH = 336;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_PADDING_X = 16;
    public static final int SHIP_CARD_HEIGHT = 52;
    public static final int BORDER_RADIUS = 8;
    public static final int GRID_GAP = 24;
    public static final int STATUS_HEIGHT = 32;
    public static final int CHIP_HEIGHT = 32;
    public static final int MANA_PIP_SIZE = 14;
    public static final int LOADOUT_GAP = 8;

    // Typography
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font FONT_SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font FONT_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_COORD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font FONT_META = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
}
