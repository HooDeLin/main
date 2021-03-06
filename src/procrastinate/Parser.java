//@@author A0126576X
package procrastinate;

import com.joestelmach.natty.DateGroup;

import procrastinate.command.AddDeadline;
import procrastinate.command.AddDream;
import procrastinate.command.AddEvent;
import procrastinate.command.Command;
import procrastinate.command.Delete;
import procrastinate.command.Done;
import procrastinate.command.EditDeadline;
import procrastinate.command.EditDream;
import procrastinate.command.EditEvent;
import procrastinate.command.EditPartial;
import procrastinate.command.EditTaskDescription;
import procrastinate.command.Exit;
import procrastinate.command.Help;
import procrastinate.command.Invalid;
import procrastinate.command.SearchDesc;
import procrastinate.command.SearchDue;
import procrastinate.command.SearchOn;
import procrastinate.command.SearchRange;
import procrastinate.command.SetPath;
import procrastinate.command.ShowAll;
import procrastinate.command.ShowDone;
import procrastinate.command.ShowOutstanding;
import procrastinate.command.ShowSummary;
import procrastinate.command.Undo;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Parser {

    private static final Logger logger = Logger.getLogger(Parser.class.getName());
    private static final com.joestelmach.natty.Parser dateParser = new com.joestelmach.natty.Parser();

    // ================================================================================
    // Message strings
    // ================================================================================

    private static final String DEBUG_PARSING_COMMAND = "Parsing command: ";

    private static final String MESSAGE_INVALID_NO_DESCRIPTION = "Please specify the description";
    private static final String MESSAGE_INVALID_LINE_NUMBER = "Please specify a valid line number";
    private static final String MESSAGE_INVALID_NO_PATH = "Please specify the save directory path";

    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_EDIT = "edit";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_DONE = "done";
    private static final String COMMAND_UNDO = "undo";
    private static final String COMMAND_SEARCH = "search";
    private static final String COMMAND_SHOW = "show";
    private static final String COMMAND_SET_PATH = "set";
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_EXIT = "procrastinate";

    private static final String COMMAND_SHORT_EDIT = "ed";
    private static final String COMMAND_SHORT_DELETE = "del";
    private static final String COMMAND_SHORT_DONE = "do";
    private static final String COMMAND_SHORT_UNDO = "un";
    private static final String COMMAND_SHORT_SEARCH = "se";
    private static final String COMMAND_SHORT_SHOW = "sh";
    private static final String COMMAND_SHORT_EXIT = "exit";

    private static final String KEYWORD_DUE_DATE = "due";
    private static final String KEYWORD_FROM_TO_DATE = "from";
    private static final String KEYWORD_TO = "to";
    private static final String KEYWORD_ON_DATE = "on";
    private static final String KEYWORD_ALL = "all";
    private static final String KEYWORD_DONE = "done";
    private static final String KEYWORD_SUMMARY = "summary";
    private static final String KEYWORD_EVENTUALLY = "eventually";
    private static final String KEYWORD_ESCAPE = "\\";

    //These are the problematic times that are unable to be handled correctly by Natty
    private static final String KEYWORD_THIS_MORNING = "this morning";
    private static final String KEYWORD_THIS_AFTERNOON = "this afternoon";
    private static final String KEYWORD_THIS_EVENING = "this evening";
    private static final String KEYWORD_THIS_NIGHT = "this night";
    private static final String KEYWORD_TONIGHT = "tonight";

    private static final String KEYWORD_THIS_MORNING_FIX = "today morning";
    private static final String KEYWORD_THIS_AFTERNOON_FIX = "today afternoon";
    private static final String KEYWORD_THIS_EVENING_FIX = "today evening";
    private static final String KEYWORD_THIS_NIGHT_FIX = "today night";
    private static final String KEYWORD_TONIGHT_FIX = "today tonight";

    private static final String WHITESPACE_STRING = " ";
    private static final char WHITESPACE_CHARACTER = ' ';
    private static final String DOUBLE_QUOTE_STRING = "\"";
    private static final char DOUBLE_QUOTE_CHARACTER = '\"';

    // ================================================================================
    // CommandStringType
    // ================================================================================

    private static enum CommandStringType {
        NO_DATE, NO_DATE_SET_PATH, ON_DATE, DUE_DATE, FROM_TO_DATE
    }

    // ================================================================================
    // Parser methods
    // ================================================================================

    /**
     * Tokenises the input string and transforms it into the appropriate command.
     */
    public static Command parse(String userInput) {
        logger.log(Level.FINE, DEBUG_PARSING_COMMAND + userInput);

        assert(userInput != null && !userInput.isEmpty());

        // Filtering userInput
        String userCommand = trimWhiteSpace(userInput);
        CommandStringType commandInputType = getCommandStringType(userCommand);
        List<Date> dateArray = getDates(userCommand, commandInputType);
        userCommand = removeDatesFromUserCommand(userCommand, commandInputType);
        // If there was a date, userCommand now comes with a trailing space.
        // This helps identify commands with no arguments: the expression
        // userCommand.equalsIgnoreCase(firstWord) will only be true if
        // no arguments were ever specified (if a date argument was specified and
        // subsequently removed, the expression will be false due to the trailing space).
        Command command;
        if (isCommandEmpty(userCommand)) {
            command = constructInvalidCommand(MESSAGE_INVALID_NO_DESCRIPTION);
        } else {
            command = constructCommand(userInput, userCommand, commandInputType, dateArray);
        }

        return command;
    }

    // ================================================================================
    // Construct command methods
    // ================================================================================

    private static Command constructCommand(String userInput, String userCommand, CommandStringType commandInputType,
            List<Date> dateArray) {
        String firstWord = getFirstWord(userCommand).toLowerCase(); // Case insensitive
        Command command = null;

        switch (firstWord) {
            case COMMAND_ADD : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "add" as an invalid command
                    // Display a helpful message (no description)
                    return constructInvalidCommand(MESSAGE_INVALID_NO_DESCRIPTION);
                }

                String[] argument = userCommand.split(WHITESPACE_STRING, 2);
                String description = argument[1];
                description = removeEscapeCharacters(description);

                if (description.isEmpty()) {
                    // Display a helpful message (no description)
                    return constructInvalidCommand(MESSAGE_INVALID_NO_DESCRIPTION);
                }

                command = constructAddCommand(commandInputType, dateArray, description);

                return command;
            }

            case COMMAND_EDIT :
            case COMMAND_SHORT_EDIT : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "edit" as an invalid command
                    // Display a helpful message (no line number given)
                    return constructInvalidCommand(MESSAGE_INVALID_LINE_NUMBER);
                }

                String[] argument = userCommand.split(WHITESPACE_STRING, 3);
                int lineNumber = 0;

                try {
                    lineNumber = Integer.parseInt(argument[1]);
                } catch (NumberFormatException e) { // Not a line number
                    // Treat "edit something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                if (argument.length <= 2 && commandInputType == CommandStringType.NO_DATE) { // Too few arguments
                    // Treat "edit 1" as a partial edit command
                    return new EditPartial(lineNumber);
                }

                String description = "";
                if (argument.length > 2) {
                    description = argument[2];
                }

                command = constructEditCommand(commandInputType, dateArray, lineNumber, description);

                return command;
            }

            case COMMAND_DELETE :
            case COMMAND_SHORT_DELETE : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "delete" as an invalid command
                    // Display a helpful message (no line number given)
                    return constructInvalidCommand(MESSAGE_INVALID_LINE_NUMBER);
                }

                String argument = userCommand.substring(firstWord.length() + 1);
                int lineNumber = 0;

                try {
                    lineNumber = Integer.parseInt(argument);
                } catch (NumberFormatException e) { // Not a line number
                    // Treat "delete something" is an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                return constructDeleteCommand(lineNumber);
            }

            case COMMAND_UNDO :
            case COMMAND_SHORT_UNDO : {
                if (!userCommand.equalsIgnoreCase(firstWord)) { // Extra arguments
                    // Treat "undo something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                return constructUndoCommand();
            }

            case COMMAND_DONE :
            case COMMAND_SHORT_DONE : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "done" as an invalid command
                    // Display a helpful message (no line number given)
                    return constructInvalidCommand(MESSAGE_INVALID_LINE_NUMBER);
                }

                String[] argument = userCommand.split(WHITESPACE_STRING, 2);
                int lineNumber = 0;

                try {
                    lineNumber = Integer.parseInt(argument[1]);
                } catch (NumberFormatException e) { // Not a line number
                    // Treat "done something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                return constructDoneCommand(lineNumber);
            }

            case COMMAND_SEARCH :
            case COMMAND_SHORT_SEARCH : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "search" as an invalid command
                    // Display a helpful message (no description)
                    return constructInvalidCommand(MESSAGE_INVALID_NO_DESCRIPTION);
                }

                command = constructSearchCommand(userCommand, commandInputType, dateArray);

                return command;
            }

            case COMMAND_SHOW :
            case COMMAND_SHORT_SHOW : {
                if (userCommand.equalsIgnoreCase(firstWord)) {
                    return constructShowOutstandingCommand();
                }

                String argument = userCommand.substring(firstWord.length() + 1);

                if (argument.equals(KEYWORD_DONE)) {
                    return constructShowDoneCommand();

                } else if (argument.equals(KEYWORD_ALL)) {
                    return constructShowAllCommand();

                } else if (argument.equals(KEYWORD_SUMMARY)) {
                    return constructShowSummaryCommand();

                } else {
                    // Treat "show something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }
            }

            case COMMAND_HELP : {
                if (!userCommand.equalsIgnoreCase(firstWord)) { // Extra arguments
                    // Treat "help something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                return constructHelpCommand();
            }

            case COMMAND_SET_PATH : {
                if (userCommand.equalsIgnoreCase(firstWord)) { // No arguments
                    // Treat "set" as an invalid command
                    // Display a helpful message (no path)
                    return constructInvalidCommand(MESSAGE_INVALID_NO_PATH);
                }

                if (!commandInputType.equals(CommandStringType.NO_DATE_SET_PATH)) {
                    return Parser.parse(putAddInFront(userInput));
                }

                command = constructSetPathCommand(userCommand);

                return command;
            }

            case COMMAND_EXIT :
            case COMMAND_SHORT_EXIT : {
                if (!userCommand.equalsIgnoreCase(firstWord)) { // Extra arguments
                    // Treat "procrastinate something" as an add command
                    // Inject add to the front of command and recurse
                    return Parser.parse(putAddInFront(userInput));
                }

                return constructExitCommand();
            }

            default: {
                // Inject add to the front of command and recurse
                return Parser.parse(putAddInFront(userInput));
            }
        }
    }

    private static Command constructAddCommand(CommandStringType commandInputType, List<Date> dateArray,
            String description) {
        Command command;

        switch (commandInputType) {
            case DUE_DATE:
            case ON_DATE:
                command = new AddDeadline(description, getStartDate(dateArray));
                break;

            case FROM_TO_DATE:
                command = new AddEvent(description, getStartDate(dateArray), getEndDate(dateArray));
                break;

            default: // NO_DATE
                command = new AddDream(description);
                break;
        }

        return command;
    }

    private static Command constructEditCommand(CommandStringType commandInputType, List<Date> dateArray,
            int lineNumber, String description) {
        Command command = null;
        description = removeEscapeCharacters(description);

        switch (commandInputType) {
            case DUE_DATE :
            case ON_DATE :
                command = new EditDeadline(lineNumber, description, getStartDate(dateArray));
                break;

            case FROM_TO_DATE :
                command = new EditEvent(lineNumber, description,
                                        getStartDate(dateArray), getEndDate(dateArray));
                break;

            default : // NO_DATE
                if (description.equals(KEYWORD_EVENTUALLY)) {
                    command = new EditDream(lineNumber);
                } else {
                    command = new EditTaskDescription(lineNumber, description);
                }
                break;
        }

        return command;
    }

    private static Command constructDeleteCommand(int lineNumber) {
        return new Delete(lineNumber);
    }

    private static Command constructUndoCommand() {
        return new Undo();
    }

    private static Command constructDoneCommand(int lineNumber) {
        return new Done(lineNumber);
    }

    private static Command constructSearchCommand(String userCommand,
                                                       CommandStringType commandInputType,
                                                       List<Date> dateArray) {

        Command command = null;
        String[] argument = userCommand.split(WHITESPACE_STRING, 2);
        String searchDescription = removeEscapeCharacters(argument[1]).trim();

        if (commandInputType.equals(CommandStringType.ON_DATE)) {

            command = new SearchOn(searchDescription, getStartDate(dateArray));

        } else if (commandInputType.equals(CommandStringType.DUE_DATE)) {

            command = new SearchDue(searchDescription, getStartDate(dateArray));

        } else if (commandInputType.equals(CommandStringType.FROM_TO_DATE)) {

            command = new SearchRange(searchDescription, getStartDate(dateArray), getEndDate(dateArray));

        } else {
            command = new SearchDesc(searchDescription);

        }

        return command;
    }

    private static Command constructShowOutstandingCommand() {
        return new ShowOutstanding();
    }

    private static Command constructShowDoneCommand() {
        return new ShowDone();
    }

    private static Command constructShowAllCommand() {
        return new ShowAll();
    }

    private static Command constructShowSummaryCommand() {
        return new ShowSummary();
    }

    private static Command constructHelpCommand() {
        return new Help();
    }

    private static Command constructSetPathCommand(String userCommand) {
        String[] pathArgs = extractSetPathArguments(userCommand);
        return new SetPath(pathArgs[0], pathArgs[1]);
    }

    private static Command constructExitCommand() {
        return new Exit();
    }

    private static Command constructInvalidCommand (String invalidMessage) {
//        return new Command(CommandType.INVALID).addDescription(invalidMessage);
        return new Invalid(invalidMessage);
    }

    // ================================================================================
    // Filtering user input methods
    // ================================================================================

    private static CommandStringType getCommandStringType(String userCommand) {
        if (isSetPath(userCommand)) {
            return CommandStringType.NO_DATE_SET_PATH;
        } else if (isKeywordDate(userCommand, KEYWORD_FROM_TO_DATE)) {
            return CommandStringType.FROM_TO_DATE;
        } else if (isKeywordDate(userCommand, KEYWORD_DUE_DATE)) {
            return CommandStringType.DUE_DATE;
        } else if (isKeywordDate(userCommand, KEYWORD_ON_DATE)) {
            return CommandStringType.ON_DATE;
        } else {
            return CommandStringType.NO_DATE;
        }
    }

    private static List<Date> getDates(String userCommand, CommandStringType commandInputType) {
        List<Date> dateList = new ArrayList<Date>();
        String keyword = null;
        if (commandInputType.equals(CommandStringType.NO_DATE) || commandInputType.equals(CommandStringType.NO_DATE_SET_PATH)) {
            return null;
        } else if (commandInputType.equals(CommandStringType.DUE_DATE)) {
            keyword = KEYWORD_DUE_DATE;
        } else if (commandInputType.equals(CommandStringType.ON_DATE)) {
            keyword = KEYWORD_ON_DATE;
        } else { //FROM_TO_DATE
            keyword = KEYWORD_FROM_TO_DATE;
        }

        String[] arguments = userCommand.split(WHITESPACE_STRING + keyword + WHITESPACE_STRING);
        String dateArguments = arguments[arguments.length - 1];
        dateArguments = replaceRelativeDates(dateArguments);
        List<DateGroup> dateGroups = dateParser.parse(dateArguments);

        dateList = fillUpDateArray(dateList, dateGroups);
        return dateList;
    }

    private static String removeDatesFromUserCommand(String userCommand, CommandStringType commandInputType) {
        String keyword = null;
        if (commandInputType.equals(CommandStringType.NO_DATE) || commandInputType.equals(CommandStringType.NO_DATE_SET_PATH)) {
            return userCommand;
        } else if (commandInputType.equals(CommandStringType.DUE_DATE)) {
            keyword = KEYWORD_DUE_DATE;
        } else if (commandInputType.equals(CommandStringType.ON_DATE)) {
            keyword = KEYWORD_ON_DATE;
        } else { // FROM_TO_DATE
            keyword = KEYWORD_FROM_TO_DATE;
        }

        int endIndex = getLastIndex(keyword, userCommand);
        if (endIndex == 0) {
            return null;
        } else {
            return userCommand.substring(0, endIndex);
            // NOT endIndex - 1; we need the trailing space! See long comment above
        }
    }

    private static boolean isSetPath(String userCommand) {
        if (!getFirstWord(userCommand).equals(COMMAND_SET_PATH) || userCommand.equals(COMMAND_SET_PATH)) {
            return false;
        }

        return isPathArgumentFormatValid(userCommand);
    }

    private static boolean isKeywordDate(String userCommand, String keyword) {
        if(!userCommand.contains(WHITESPACE_STRING + keyword + WHITESPACE_STRING)) {
            return false;
        }

        String[] arguments = userCommand.split(WHITESPACE_STRING + keyword + WHITESPACE_STRING);
        String lastArgument = arguments[arguments.length - 1];
        List<DateGroup> dateGroups = dateParser.parse(lastArgument);

        if(keyword.equals(KEYWORD_FROM_TO_DATE) && !lastArgument.contains(WHITESPACE_STRING + KEYWORD_TO + WHITESPACE_STRING)) {
            return false;
        }

        if (!hasDates(dateGroups)) {
            return false;
        }

        int numberOfDates = dateGroups.get(0).getDates().size();

        // natty returns dates even if the dates are between words
        // we need to make sure there are no excess words before and after the dates
        if (((keyword.equals(KEYWORD_DUE_DATE) || keyword.equals(KEYWORD_ON_DATE)) && numberOfDates == 1)
                || ((keyword.equals(KEYWORD_FROM_TO_DATE)) && numberOfDates == 2)) {
            return dateGroups.get(0).getPosition() == 1 && dateGroups.get(0).getText().equals(lastArgument);
        } else {
            return false;
        }
    }

    private static boolean isPathArgumentFormatValid(String userCommand) {
        if (userCommand.contains(DOUBLE_QUOTE_STRING + DOUBLE_QUOTE_STRING)) {//Guard condition against two double quotes
            return false;
        }
        String[] arguments = userCommand.split(DOUBLE_QUOTE_STRING);
        if (arguments.length == 1) {
            // The user command has no quotes or have a quote at the last character
            // Therefore, we need to check if the last character is a quote
            // We also need to check if the format only has two or three arguments
            return !userCommand.contains(DOUBLE_QUOTE_STRING) && userCommand.split(WHITESPACE_STRING).length <= 3;
        }

        if (arguments[0].charAt(arguments[0].length() - 1) != WHITESPACE_CHARACTER) {
            return false;
        }

        if (arguments.length == 2) {
            // We need to check if the last character is a quote
            return userCommand.charAt(userCommand.length() - 1) == DOUBLE_QUOTE_CHARACTER;
        } else if (arguments.length == 3) {
            // The second argument is unprotected by quotes
            // To make sure the second argument is valid, we need to check if
            // the first character is a space and the argument has two words only
            return String.valueOf(arguments[2].charAt(0)).equals(WHITESPACE_STRING)
                    && arguments[2].split(WHITESPACE_STRING).length == 2;
        } else if (arguments.length == 4) {
            // Both arguments are protected by quotes
            // Check if the last character is a quote
            // the two arguments are separated by a WHITESPACE_STRING
            return userCommand.charAt(userCommand.length() - 1) == DOUBLE_QUOTE_CHARACTER
                    && arguments[2].equals(WHITESPACE_STRING);
        } else {
            // Too many arguments
            return false;
        }
    }

    // ================================================================================
    // Utility methods
    // ================================================================================

    private static List<Date> fillUpDateArray(List<Date> dateList, List<DateGroup> dateGroups) {
        boolean isEventDate = dateGroups.get(0).getDates().size() == 2;

        if (!dateGroups.get(0).isTimeInferred()) {
            dateList.add(0, dateGroups.get(0).getDates().get(0));
            if (dateGroups.get(0).getDates().size() > 1) {
                dateList.add(1, dateGroups.get(0).getDates().get(1));
            }
            return dateList;
        }

        Calendar date = Calendar.getInstance();
        if (isEventDate) {
            Date newDate = setStartDate(dateGroups.get(0).getDates().get(0), date);
            dateList.add(0, newDate);
            Date endDate = setEndDate(dateGroups.get(0).getDates().get(1), date);
            dateList.add(1, endDate);
        } else {
            Date newDate = setEndDate(dateGroups.get(0).getDates().get(0), date);
            dateList.add(0, newDate);
        }

        return dateList;
    }

    private static Date setStartDate(Date date, Calendar calendarDate) {
        calendarDate.setTime(date);
        calendarDate.set(Calendar.HOUR_OF_DAY, 8);
        calendarDate.set(Calendar.MINUTE, 0);
        calendarDate.set(Calendar.SECOND, 0);
        calendarDate.set(Calendar.MILLISECOND, 0);
        Date newDate = calendarDate.getTime();
        return newDate;
    }

    private static Date setEndDate(Date date, Calendar calendarDate) {
        calendarDate.setTime(date);
        calendarDate.set(Calendar.HOUR_OF_DAY, 23);
        calendarDate.set(Calendar.MINUTE, 59);
        calendarDate.set(Calendar.SECOND, 0);
        calendarDate.set(Calendar.MILLISECOND, 0);
        Date newDate = calendarDate.getTime();
        return newDate;
    }

    /**
     *  There exists bugs that cause Natty to parse some dates wrongly
     *  We replace these dates to dates that are more specific but yields
     *  the same result.
     */
    private static String replaceRelativeDates(String dateArguments) {
        dateArguments = dateArguments.toLowerCase();
        dateArguments = dateArguments.replace(KEYWORD_THIS_MORNING, KEYWORD_THIS_MORNING_FIX);
        dateArguments = dateArguments.replace(KEYWORD_THIS_AFTERNOON, KEYWORD_THIS_AFTERNOON_FIX);
        dateArguments = dateArguments.replace(KEYWORD_THIS_EVENING, KEYWORD_THIS_EVENING_FIX);
        dateArguments = dateArguments.replace(KEYWORD_THIS_NIGHT, KEYWORD_THIS_NIGHT_FIX);
        dateArguments = dateArguments.replace(KEYWORD_TONIGHT, KEYWORD_TONIGHT_FIX);
        return dateArguments;
    }

    private static String removeEscapeCharacters(String userCommand) {
        String removedString = "";
        boolean isPreviousEscapeChar = false;
        for (int i = 0; i < userCommand.length(); i ++) {
            String currentChar = String.valueOf(userCommand.charAt(i));
            if(!isPreviousEscapeChar && currentChar.equals(KEYWORD_ESCAPE)) {
                isPreviousEscapeChar = true;
            } else {
                removedString += currentChar;
                isPreviousEscapeChar = false;
            }
        }
        return removedString;
    }

    private static String[] extractSetPathArguments(String userCommand) {
        // We assume that the user command is in a valid set path format
        String[] result = new String[2];
        String[] arguments = userCommand.split(DOUBLE_QUOTE_STRING);
        if (arguments.length == 1) { // There are no double quotes
            String[] pathArguments = userCommand.split(WHITESPACE_STRING);
            try {
                result[0] = pathArguments[1];
                result[1] = pathArguments[2];
            } catch (Exception e) {

            }
        } else if (arguments.length == 2) {
            // We need to take care two situations
            // 1) set <path dir> "<filename>"
            // 2) set "<path dir>"
            if (trimWhiteSpace(arguments[0]).equals(COMMAND_SET_PATH)) {
                result[0] = arguments[1];
            } else {
                result[0] = arguments[0].split(WHITESPACE_STRING)[1];
                result[1] = arguments[1];
            }
        } else if (arguments.length == 3) {
            // Only possibility
            // set "<path dir>" <filename>
            result[0] = arguments[1];
            result[1] = trimWhiteSpace(arguments[2]);
        } else if (arguments.length == 4) {
            // Only possibility
            // set "<path dir>" "<filename>"
            result[0] = arguments[1];
            result[1] = arguments[3];
        }
        return result;
    }

    private static String trimWhiteSpace(String userInput) {
        return userInput.trim().replaceAll("\\s+", WHITESPACE_STRING);
    }

    private static int getLastIndex(String keyword, String userCommand) {
        int lastIndex = userCommand.lastIndexOf(WHITESPACE_STRING + keyword + WHITESPACE_STRING);
        if(lastIndex == -1) {
            return -1;
        } else {
            return lastIndex + 1;
        }
    }

    private static boolean isCommandEmpty(String userCommand) {
        return userCommand == null || userCommand.isEmpty();
    }

    private static String getFirstWord(String userCommand) {
        return userCommand.split(WHITESPACE_STRING)[0];
    }

    private static String putAddInFront(String userInput) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(COMMAND_ADD);
        stringBuilder.append(WHITESPACE_STRING);
        stringBuilder.append(userInput);
        return stringBuilder.toString();
    }

    private static boolean hasDates(List<DateGroup> groups) {
        return !groups.isEmpty();
    }

    private static Date getStartDate(List<Date> dateArray) {
        return dateArray.get(0);
    }

    private static Date getEndDate(List<Date> dateArray) {
        return dateArray.get(1);
    }
}
