import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class ChannelMenu {
    private  String noteId;
    private final Client client;
    private NoteMenu menu;

    public ChannelMenu(final Client client) {
        this.client = client;
    }

    public int start () {
        int action;
        String name = client.getProperties().getProperty("channel.action");
        String note = client.getProperties().getProperty("note.name");
        if (name != null && name == "add") {
            noteId = addNewNote();
        } else if (note != null) {
            noteId = note;
        } else {
            action = show();
            if((action == 0) || (action == 1)) {
                return action;
            }
        }
        System.out.println("Start working with " + noteId);
        menu = new NoteMenu(client, noteId);
        do {
            action = menu.start();
            if (action == 1) {
                return 1;
            }
            action = show();
            if (action == 1) {
                return 1;
            }
        } while (action != 0);
        return 0;
    }

    private String addNewNote() {
        // TODO: call operator SC
        String assetId = "asset" + Instant.now().toEpochMilli();
        System.out.println("\n--> Submit transaction: CreateNote, " + assetId + " from 10 to 100");

        try {
            long blockNumber = client.createNote(assetId);
            System.out.println("\n*** CreateNote committed successfully");
            System.out.println("\n***Added to block " + blockNumber);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return assetId;
    }

    @SuppressWarnings("resource")
    public int show() {
        String prompt = "0. Back to menu\n1. Exit\n2. Add note\n3. Show note list\n4. Select note\n5. Show channel events";
        System.out.println(prompt);
        @SuppressWarnings("resource")
        Scanner in = new Scanner(System.in);
        int action = in.nextInt();
        while (action != 1) {
            switch(action) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    noteId = addNewNote();
                    return 2;
                case 3:
                    listNote();
                    break;
                case 4:
                    chooseNote(in);
                    return 2;
                case 5:
                    showEvents();
                    break;
                default:
                    break;
            }
            System.out.println(prompt);
            action = in.nextInt();
        }
        return action;
    }

    private void showEvents() {
        client.replayChaincodeEvents(0);
    }

    private void listNote() {
        Map<String, String> notes = client.getNotes();
        for (Entry<String, String> note : notes.entrySet()) {
            System.out.println(note.getKey());
            System.out.println(note.getValue());
        }
    }

    private void chooseNote(Scanner in) {
        var notes =  client.getNoteList();
        System.out.println("Select note:");
        for (int i=0; i < notes.length; i++) {
            System.out.println(i + 1 + ". " + notes[i]);
        }
        int choice = in.nextInt();
        if (choice > 0 && choice <= notes.length) {
            String name = notes[choice - 1];
            if (name.equals(noteId)) {
                System.out.println("Note " + name + " is already selected");
            } else {
                noteId = name;
                System.out.println("Note " + name + " is selected");
            }
        }
    }
}
