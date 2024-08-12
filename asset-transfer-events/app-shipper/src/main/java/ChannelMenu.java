import java.time.Instant;
import java.util.Scanner;

public class ChannelMenu {
    private  String noteId;
    private final Client client;
    private NoteMenu menu;

    public ChannelMenu(final Client client) {
        this.client = client;
    }

    public int start () {
        String name = client.getProperties().getProperty("channel.action");
        if (name != null && name == "add") {
            noteId = addNewNote();
        } else {
            int action = show();
            if((action == 0) || (action == 1)) {
                return action;
            }
        }
        System.out.println("Start working with " + noteId);
        menu = new NoteMenu(client, noteId);
        int action;
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

    public int show() {
        String prompt = "0. Back to menu\n1. Exit\n2. Add note\n3. Show note list\n4. Show channel events";
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
                    //chooseNote(in);
                    break;
                case 4:
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
}
