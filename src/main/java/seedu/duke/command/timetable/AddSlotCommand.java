package seedu.duke.command.timetable;

import seedu.duke.ItemList;
import seedu.duke.Storage;
import seedu.duke.Ui;
import seedu.duke.command.Command;
import seedu.duke.exception.DukeException;
import seedu.duke.exception.DukeExceptionType;
import seedu.duke.bookmark.Bookmark;
import seedu.duke.bookmark.BookmarkList;
import seedu.duke.slot.Slot;
import seedu.duke.slot.SlotList;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;


/**
 * Represents the user command exit the Duke program.
 */
public class AddSlotCommand extends Command {
    public static final String ADD_KW = "add";
    public LocalTime startTime;
    public LocalTime endTime;
    public String day;
    public String title;

    /**
     * Constructs a new AddSlotCommand instance and stores the information of the slot given by the input.
     *
     * @param command The user input command.
     * @throws DukeException if input command is invalid.
     */
    public AddSlotCommand(String command) throws DukeException {
        assert command.startsWith(ADD_KW);
        String details = command.substring(ADD_KW.length());
        if (details.isBlank()) {
            throw new DukeException(DukeExceptionType.EMPTY_COMMAND, ADD_KW);
        }


        String[] parts = command.split(" ");
        try {
            startTime = LocalTime.parse(parts[1]);
            endTime = LocalTime.parse(parts[2]);
            day = parts[3];
            title = command.substring(command.indexOf(parts[3]) + parts[3].length()).trim();
        } catch (DateTimeParseException e) {
            throw new DukeException(DukeExceptionType.INVALID_TIME_FORMAT);
        } catch (IndexOutOfBoundsException e) {
            throw new DukeException(DukeExceptionType.INVALID_SLOT_INPUT);
        }
    }

    /**
     * Adds the slot to the slot list and saves the slots list in the text file.
     *
     * @param slots The list of slots.
     * @param ui The user interface.
     * @param slotStorage The storage for saving and loading.
     */
    @Override
    public void execute(BookmarkList bookmarks, SlotList slots, Ui ui, Storage bookmarkStorage,
                        Storage slotStorage) throws DukeException {
        Slot slot = new Slot(startTime, endTime, day, title);
        slots.addSlot(slot);
        ui.print("Added slot: " + day + " [" + startTime + "-" + endTime + "] "
                + title + System.lineSeparator());
        try {
            slotStorage.save(slots.getData());
        } catch (DukeException e) {
            e.printStackTrace();
        }
    }
}
