package boat.carpetorgaddition.wheel.text;

public final class LocalizationKeys {
    public static final LocalizationKey COMMAND = LocalizationKey.of("command");
    public static final LocalizationKey LOGGER = LocalizationKey.of("logger");
    private static final LocalizationKey OPERATION = LocalizationKey.of("operation");
    private static final LocalizationKey RULE = LocalizationKey.of("rule");
    private static final LocalizationKey DATA = LocalizationKey.of("data");
    private static final LocalizationKey TIME = LocalizationKey.of("time");
    private static final LocalizationKey ARGUMENT = LocalizationKey.of("argument");
    private static final LocalizationKey ITEM = LocalizationKey.of("item");
    private static final LocalizationKey BUTTON = LocalizationKey.of("button");
    private static final LocalizationKey DIMENSION = LocalizationKey.of("dimension");
    private static final LocalizationKey LITERAL = LocalizationKey.of("literal");
    private static final LocalizationKey FILE = LocalizationKey.of("file");
    private static final LocalizationKey MISC = LocalizationKey.of("misc");
    private static final LocalizationKey DIALOG = LocalizationKey.of("dialog");
    private static final LocalizationKey RENDER = LocalizationKey.of("render");
    private static final LocalizationKey KEYBOARD = LocalizationKey.of("keyboard");
    private static final LocalizationKey CUSTOM_CLICK_ACTION = LocalizationKey.of("custom_click_action");

    public static class Operation {
        public static final LocalizationKey NOT_FAKE_PLAYER = OPERATION.then("not_fake_player");
        public static final LocalizationKey SELF_OR_FAKE_PLAYER = OPERATION.then("self_or_fake_player");
        public static final LocalizationKey OFFLINE_PLAYER_NAME = OPERATION.then("offline_player_name");
        public static final LocalizationKey UNABLE_TO_PARSE_STRING_TO_UUID = OPERATION.then("unable_to_parse_string_to_uuid");
        public static final LocalizationKey INSUFFICIENT_PERMISSIONS = OPERATION.then("insufficient_permissions");
        public static final LocalizationKey WAIT_LAST = OPERATION.then("wait_last");
        private static final LocalizationKey ERROR = OPERATION.then("error");
        private static final LocalizationKey TIMEOUT = OPERATION.then("timeout");
        private static final LocalizationKey PAGE = OPERATION.then("page");
        private static final LocalizationKey OPEN_INVENTORY = OPERATION.then("open_inventory");
        private static final LocalizationKey QUERY_PLAYER_NAME = OPERATION.then("query_player_name");

        public static class Page {
            public static final LocalizationKey INVALID_INDEX = PAGE.then("invalid_index");
            public static final LocalizationKey NON_EXISTENT = PAGE.then("non_existent");
        }

        public static class Error {
            public static final LocalizationKey IO = ERROR.then("io");
        }

        public static class Timeout {
            public static final LocalizationKey TASK = TIMEOUT.then("task");
            public static final LocalizationKey OPERATION = TIMEOUT.then("operation");
        }

        public static class OpenInventory {
            public static final LocalizationKey HOVER = OPEN_INVENTORY.then("hover");
        }

        public static class QueryPlayerName {
            public static final LocalizationKey START = QUERY_PLAYER_NAME.then("start");
            public static final LocalizationKey SUCCESS = QUERY_PLAYER_NAME.then("success");
            public static final LocalizationKey FAIL = QUERY_PLAYER_NAME.then("fail");
            private static final LocalizationKey HOVER = QUERY_PLAYER_NAME.then("hover");

            public static class Hover {
                public static final LocalizationKey FIRST = QueryPlayerName.HOVER.then("first");
                public static final LocalizationKey SECOND = QueryPlayerName.HOVER.then("second");
            }
        }
    }

    public static class Rule {
        private static final LocalizationKey MESSAGE = RULE.then("message");
        private static final LocalizationKey VALIDATE = RULE.then("validate");
        public static final LocalizationKey COMPATIBILITY = RULE.then("compatibility");

        public static class Message {
            public static final LocalizationKey DISABLE_RESPAWN_BLOCKS_EXPLODE = MESSAGE.then("disableRespawnBlocksExplode");
            public static final LocalizationKey CCE_UPDATE_SUPPRESSION = MESSAGE.then("CCEUpdateSuppression");
            public static final LocalizationKey DISPLAY_PLAYER_SUMMONER = MESSAGE.then("displayPlayerSummoner");
            public static final LocalizationKey PLAYER_MANAGER_FORCE_COMMENT = MESSAGE.then("playerManagerForceComment");
        }

        public static class Validate {
            public static final LocalizationKey INVALID_VALUE = VALIDATE.then("invalid_value");
            public static final LocalizationKey GREATER_THAN = VALIDATE.then("greater_than");
            public static final LocalizationKey LESS_THAN = VALIDATE.then("less_than");
            public static final LocalizationKey GREATER_THAN_OR_EQUAL = VALIDATE.then("greater_than_or_equal");
            public static final LocalizationKey LESS_THAN_OR_EQUAL = VALIDATE.then("less_than_or_equal");
            public static final LocalizationKey GREATER_THAN_OR_EQUAL_OR_NUMBER = VALIDATE.then("greater_than_or_equal_or_number");
            public static final LocalizationKey BETWEEN_TWO_NUMBER_OR_NUMBER = VALIDATE.then("between_two_number_or_number");
            public static final LocalizationKey VALID_OPTIONS = VALIDATE.then("valid_options");
        }

        @SuppressWarnings("unused")
        public static class Compatibility {
            public static final LocalizationKey WARNING = COMPATIBILITY.then("warning");
            public static final LocalizationKey STILL_OPEN = COMPATIBILITY.then("still_open");
            public static final LocalizationKey KEEP_CLOSED = COMPATIBILITY.then("keep_closed");
        }
    }

    public static class Data {
        public static final LocalizationKey UNABLE_TO_PARSE = DATA.then("unable_to_parse");
        private static final LocalizationKey TYPE = DATA.then("type");

        public static class Type {
            public static final LocalizationKey STRING = TYPE.then("string");
            public static final LocalizationKey INTEGER = TYPE.then("integer");
            public static final LocalizationKey LONG = TYPE.then("long");
            public static final LocalizationKey BOOLEAN = TYPE.then("boolean");
            public static final LocalizationKey DOUBLE = TYPE.then("double");
            public static final LocalizationKey FLOAT = TYPE.then("float");
            public static final LocalizationKey ENUM = TYPE.then("enum");
        }
    }

    public static class Argument {
        private static final LocalizationKey PLAYER = ARGUMENT.then("player");
        private static final LocalizationKey OBJECT = ARGUMENT.then("object");

        public static class Player {
            public static final LocalizationKey TOOMANY = PLAYER.then("toomany");
        }

        public static class Object {
            public static final LocalizationKey INVALID_PATTERN = OBJECT.then("invalid_pattern");
            public static final LocalizationKey BROAD = OBJECT.then("broad");
            public static final LocalizationKey MISMATCH = OBJECT.then("mismatch");
        }
    }

    public static class Time {
        public static final LocalizationKey TICK = TIME.then("tick");
        public static final LocalizationKey SECOND = TIME.then("second");
        public static final LocalizationKey MINUTE = TIME.then("minute");
        public static final LocalizationKey HOUR = TIME.then("hour");
        public static final LocalizationKey MINUTE_SECOND = TIME.then("minute_second");
        public static final LocalizationKey HOUR_MINUTE = TIME.then("hour_minute");
        public static final LocalizationKey FORMAT = TIME.then("format");
    }

    public static class Item {
        public static final LocalizationKey ITEM = LocalizationKeys.ITEM.then("item");
        public static final LocalizationKey DROPS = LocalizationKeys.ITEM.then("drops");
        public static final LocalizationKey REMAINDER = LocalizationKeys.ITEM.then("remainder");
        public static final LocalizationKey GROUP = LocalizationKeys.ITEM.then("group");
        public static final LocalizationKey COUNT = LocalizationKeys.ITEM.then("count");
        public static final LocalizationKey ANY_ITEM = LocalizationKeys.ITEM.then("any_item");
        public static final LocalizationKey PLACEHOLDER = LocalizationKeys.ITEM.then("placeholder");
    }

    public static class Literal {
        public static final LocalizationKey TRUE = LITERAL.then("true");
        public static final LocalizationKey FALSE = LITERAL.then("false");
    }

    public static class Button {
        public static final LocalizationKey HERE = BUTTON.then("here");
        public static final LocalizationKey INPUT = BUTTON.then("input");
        public static final LocalizationKey RUN_COMMAND = BUTTON.then("run_command");
        public static final LocalizationKey LOGIN = BUTTON.then("login");
        public static final LocalizationKey LOGOUT = BUTTON.then("logout");
        public static final LocalizationKey HIGHLIGHT = BUTTON.then("highlight");
        public static final LocalizationKey NAVIGATE = BUTTON.then("navigate");
        public static final LocalizationKey NAVIGATE_HOVER = NAVIGATE.then("hover");
        public static final LocalizationKey BACK = BUTTON.then("back");
        public static final LocalizationKey CONFIRM = BUTTON.then("confirm");
        public static final LocalizationKey CLOSE = BUTTON.then("close");
        public static final LocalizationKey PREV_PAGE = BUTTON.then("prev_page");
        public static final LocalizationKey NEXT_PAGE = BUTTON.then("next_page");
        public static final LocalizationKey HOTBAR = BUTTON.then("hotbar");
        public static final LocalizationKey ON = BUTTON.then("on");
        public static final LocalizationKey OFF = BUTTON.then("off");
        private static final LocalizationKey ACTION = BUTTON.then("action");

        public static class Action {
            private static final LocalizationKey STOP = ACTION.then("stop");
            private static final LocalizationKey ATTACK = ACTION.then("attack");
            private static final LocalizationKey USE = ACTION.then("use");

            public static class Stop {
                public static final LocalizationKey LEFT = STOP.then("left");
                public static final LocalizationKey RIGHT = STOP.then("right");
            }

            public static class Attack {
                public static final LocalizationKey INTERVAL = ATTACK.then("interval");
                public static final LocalizationKey CONTINUOUS = ATTACK.then("continuous");
            }

            public static class Use {
                public static final LocalizationKey CONTINUOUS = USE.then("continuous");
            }
        }
    }

    public static class Dimension {
        public static final LocalizationKey OVERWORLD = DIMENSION.then("overworld");
        public static final LocalizationKey THE_NETHER = DIMENSION.then("the_nether");
        public static final LocalizationKey THE_END = DIMENSION.then("the_end");
    }

    public static class File {
        public static final LocalizationKey INVALID_NAME = FILE.then("invalid_name");
    }

    public static class Misc {
        public static final LocalizationKey UNDEFINED = MISC.then("undefined");
        public static final LocalizationKey OPERATOR = MISC.then("operator");
        public static final LocalizationKey INVENTORY = MISC.then("inventory");
        public static final LocalizationKey ENDER_CHEST = MISC.then("ender_chest");
        public static final LocalizationKey SELF = MISC.then("self");
    }

    public static class Dialog {
        private static final LocalizationKey TEXT = DIALOG.then("text");
        private static final LocalizationKey TITLE = DIALOG.then("title");
        private static final LocalizationKey TEXTBOX = DIALOG.then("textbox");

        public static class Text {
            public static final LocalizationKey VERSION = TEXT.then("version");
        }

        public static class Title {
            public static final LocalizationKey FUNCTION = TITLE.then("function");
            public static final LocalizationKey OPEN_INVENTORY = TITLE.then("open_inventory");
            public static final LocalizationKey QUERY_PLAYER_NAME = TITLE.then("query_player_name");
            public static final LocalizationKey ERROR = TITLE.then("error");
        }

        public static class Textbox {
            public static final LocalizationKey UUID = TEXTBOX.then("uuid");
        }
    }

    public static class CustomClickAction {
        public static final LocalizationKey EXPIRED = CUSTOM_CLICK_ACTION.then("expired");
    }

    public static class Render {
        public static final LocalizationKey WAYPOINT = RENDER.then("waypoint");
    }

    public static class Keyboard {
        public static final LocalizationKey WAYPOINT = KEYBOARD.then("waypoint");
    }
}
