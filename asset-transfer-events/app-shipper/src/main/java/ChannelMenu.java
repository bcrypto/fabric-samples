import java.time.Instant;
import java.util.Scanner;

import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.SubmitException;

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
        while(menu.start() > 1) {
            int action = show();
            if (action == 1) {
                return 1;
            }
        }
        return 0;
    }

    private String addNewNote() {
        // TODO: call operator SC
        String assetId = "asset" + Instant.now().toEpochMilli();
        System.out.println("\n--> Submit transaction: CreateNote, " + assetId + " from 10 to 100");

        try {
            long blockNumber = client.createNote(assetId);

            System.out.println("\n***Added to block " + blockNumber);
        } catch (SubmitException | EndorseException | CommitStatusException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
        return assetId;
    }

    public int show() {
        String prompt = "0. Back to menu\n1. Exit\n2. Add note\n3. Show note list";
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
                default:
                    break;
            }
            System.out.println(prompt);
            action = in.nextInt();
        }
        return action;
    }
}
