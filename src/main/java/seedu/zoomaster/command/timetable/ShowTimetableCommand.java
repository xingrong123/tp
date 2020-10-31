package seedu.zoomaster.command.timetable;

import seedu.zoomaster.Ui;
import seedu.zoomaster.bookmark.BookmarkList;
import seedu.zoomaster.command.Command;
import seedu.zoomaster.exception.ZoomasterException;
import seedu.zoomaster.exception.ZoomasterExceptionType;
import seedu.zoomaster.slot.Day;
import seedu.zoomaster.slot.Module;
import seedu.zoomaster.slot.Slot;
import seedu.zoomaster.slot.SlotContainer;
import seedu.zoomaster.slot.Timetable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ShowTimetableCommand extends Command {
    public static final String SHOW_KW = "show";
    private String day;
    private boolean showBookmarks = false;
    private String module = null;

    /**
     * Constructs a new ShowTimetableCommand instance.
     */
    public ShowTimetableCommand(String command) throws ZoomasterException {
        assert command.startsWith(SHOW_KW) : "command should start with show keyword";
        if (command.compareTo(SHOW_KW) == 0) {
            day = "ALL";
        } else {
            if (command.charAt(SHOW_KW.length()) != ' ') {
                throw new ZoomasterException(ZoomasterExceptionType.UNKNOWN_INPUT);
            }
            String details = command.substring(SHOW_KW.length() + 1).trim();
            if (details.toLowerCase().equals("today")) {
                day = Day.getDayToday();
            }
            if (Day.isDay(details)) {
                day = Day.getDayFromCommand(details);
            } else {
                String[] something = details.split(" ", 2);
                module = something[0];
                if (something.length == 2) {
                    if (something[1].compareTo("bookmarks") == 0) {
                        showBookmarks = true;
                    } else {
                        throw new ZoomasterException(ZoomasterExceptionType.UNKNOWN_INPUT);
                    }
                }
            }
        }
    }

    @Override
    public void execute(BookmarkList bookmarks, Timetable timetable, Ui ui) throws ZoomasterException {
        String message = "";
        List<Module> modules = timetable.getFullModuleList();
        if (day != null) { // "show" and "show day" and "show today"
            List<Slot> list = new ArrayList<>(timetable.getFullSlotList());
            message += getMessageLessonAtTime(modules, list, day);
        } else if (isShowModule()) {
            if (!timetable.moduleExists(module)) {
                throw new ZoomasterException(ZoomasterExceptionType.INVALID_MODULE);
            }
            Module matchedModule = timetable.getModule(module);
            message += getMessageForModule(matchedModule);
        } else if (isShowModuleBookmarks()) {
            if (!timetable.moduleExists(module)) {
                throw new ZoomasterException(ZoomasterExceptionType.INVALID_MODULE);
            }
            Module matchedModule = timetable.getModule(module);
            message += matchedModule.getBookmarks();
        }
        ui.print(message);
    }

    private String getMessageSlotsInADay(List<Module> modules, List<Slot> slots, String day) {
        StringBuilder message = new StringBuilder();
        boolean hasSlotOnDay = false;
        boolean hasIndicatorOnDay = false;
        if (day.equals(Day.getDayToday())) {
            hasIndicatorOnDay = true;
        }

        ArrayList<Slot> thisDaySlots = new ArrayList<>();
        ArrayList<String> thisDayModuleCodesList = new ArrayList<>();
        for (Slot s: slots) {
            for (Module module : modules) {
                if (module.slotExists(s) && s.getDay().equals(day)) {
                    thisDaySlots.add(s);
                    thisDayModuleCodesList.add(module.getModuleCode());
                }
            }
        }
        SlotContainer slotContainer = new SlotContainer(thisDaySlots, thisDayModuleCodesList);
        SlotContainer sortedSlotContainer = SlotContainer.sortSlotsByTime(slotContainer);
        ArrayList<Slot> sortedSlots = sortedSlotContainer.getSlotList();
        ArrayList<String> sortedModuleCodes = sortedSlotContainer.getModuleCodesList();

        for (int i = 0; i < sortedSlots.size(); i++) {
            if (hasLessonNow(sortedSlots.get(i)) && !sortedSlots.get(i).getTitle().equals("<empty slot>")) {
                message.append(getHighlighBoxUpperMessage());
                message.append(i + 1).append(". ")
                        .append(sortedSlots.get(i).toString()).append(" ")
                        .append(sortedModuleCodes.get(i)).append("\n");
                message.append(getHighlighBoxLowerMessage());
                hasIndicatorOnDay = false;
            } else {
                if (sortedSlots.get(i).getStartTime().isAfter(LocalTime.now())
                        && hasIndicatorOnDay) {
                    message.append(getIndicatorMessage());
                    hasIndicatorOnDay = false;
                }
                message.append(i + 1).append(". ")
                        .append(sortedSlots.get(i).toString()).append(" ")
                        .append(sortedModuleCodes.get(i)).append("\n");
            }
            hasSlotOnDay = true;
        }

        if (!hasSlotOnDay) {
            message.append("No lessons" + "\n");
        }

        if (hasIndicatorOnDay) {
            message.append(getIndicatorMessage());
        }

        message.append("\n");
        return message.toString();
    }

    private String getMessageTimetable(List<Module> modules, List<Slot> slots) {
        StringBuilder message = new StringBuilder();
        for (Day day: Day.values()) {
            message.append(day.toString()).append("\n");
            message.append(getMessageSlotsInADay(modules, slots, day.toString()));
        }
        return message.toString();
    }

    private String getMessageLessonAtTime(List<Module> modules, List<Slot> slots,
                                          String dayInput) throws ZoomasterException {
        String message = "";
        if (slots.isEmpty()) {
            throw new ZoomasterException(ZoomasterExceptionType.EMPTY_TIMETABLE);
        } else if (dayInput == null) {
            throw new ZoomasterException(ZoomasterExceptionType.INVALID_TIMETABLE_DAY);
        } else if (dayInput.compareTo("ALL") == 0) {
            return getMessageTimetable(modules,slots);
        }
        message += "Lessons for " + dayInput + "\n";
        message += getMessageSlotsInADay(modules, slots, dayInput);
        return message;
    }

    private String getMessageForModule(Module module) {
        String message = "";
        List<Slot> slots = module.getSlotList();
        if (!slots.isEmpty()) {
            message += module.getModuleCode() + "\n";
            for (int i = 0; i < slots.size(); i++) {
                Slot slot = slots.get(i);
                String dayString = (isShowModule() ? slot.getDay() + " " : "");
                message += "  " + (i + 1) + ". " + dayString + slot.toString() + "\n";
            }
        } else {
            message += "no slots for " + module.getModuleCode() + "\n";
        }
        return message;
    }

    public static boolean hasLessonNow(Slot slot) {
        boolean isOverlap = false;
        LocalTime timeNow = LocalTime.now();
        if (slot.getStartTime().isBefore(timeNow) && slot.getEndTime().isAfter(timeNow)
                && Day.getDayToday().equals(slot.getDay())) {
            isOverlap = true;
        }
        return isOverlap;
    }

    public static String getIndicatorMessage() {
        DateTimeFormatter hoursAndMinutes = DateTimeFormatter.ofPattern("HH:mm");
        String currentTimeMessage = "<----" + "Current Time: " + LocalTime.now().format(hoursAndMinutes)
                + "---->" + "\n";

        return "\u001b[34m" + currentTimeMessage + "\u001b[0m";
    }

    public static String getHighlighBoxUpperMessage() {
        String message = "[====" + "Lesson now" + "====]" + "\n";

        return "\u001b[32m" + message + "\u001b[0m";
    }

    public static String getHighlighBoxLowerMessage() {
        String message = "[==================]" + "\n";

        return "\u001b[32m" + message + "\u001b[0m";
    }

    private boolean isShowModuleBookmarks() {
        return module != null && showBookmarks;
    }

    private boolean isShowModule() {
        return module != null && !showBookmarks;
    }
}
