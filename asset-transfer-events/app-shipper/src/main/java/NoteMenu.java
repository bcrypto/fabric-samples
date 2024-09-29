import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.w3c.dom.Document;

public class NoteMenu {
    private String noteId;
    private final Client client;

     public NoteMenu(final Client client, String noteId) {
        this.client = client;
        this.noteId = noteId;
    }

    public void setNodeId(String id) {
        this.noteId = id;
    }

    public int start () {
        String name = client.getProperties().getProperty("note.action");
        if (name != null) {
            switch (name) {
                case "add":
                    addNewMsg();
                    break;
                case "export":
                    exportNote();
                    break;
                case "sign":
                    signMsg();
                    break;
                default:
                    break;
            }
        } else {
            int action = show();
            if((action == 0) || (action == 1)) {
                return action;
            }
        }
        System.out.println("Start working with " + noteId);
        return 0;
    }

    public int show() {
        String prompt = "0. Back to menu\n1. Exit\n2. Add message\n3. Export note\n" 
            +"4. Sign message\n5. List messages";
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
                    addNewMsg();
                    break;
                case 3:
                    exportNote();
                    break;
                case 4:
                    signMsg();
                    break;
                case 5:
                    listMsg();
                    break;
                default:
                    break;
            }
            System.out.println(prompt);
            action = in.nextInt();
        }
        return action;
    }

    private void addNewMsg() {
        String filename = client.getProperties().getProperty("msg.input");
        if (filename == null) {
            System.out.println("Enter input file name:");
            @SuppressWarnings("resource")
            Scanner in = new Scanner(System.in);
            filename = in.nextLine();
        }
        File file = new File(filename);
        try {
            Document doc = XmlUtils.loadXML(file);
            client.addMessage(noteId, doc);
        } catch (FileNotFoundException e) {
            System.out.println("File is not found: " + filename);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void exportNote() {
        String filename = client.getProperties().getProperty("msg.output");
        if (filename == null) {
            System.out.println("Enter input file name:");
            @SuppressWarnings("resource")
            Scanner in = new Scanner(System.in);
            filename = in.nextLine();
        }
        try{
            String delnote = client.getAsset(noteId);
            FileOutputStream outputStream = new FileOutputStream(filename);
            outputStream.write(delnote.getBytes());
            outputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void signMsg() {
        String reference = client.getProperties().getProperty("msg.ref");
        if (reference == null) {
            System.out.println("Enter message id:");
            @SuppressWarnings("resource")
            Scanner in = new Scanner(System.in);
            reference = in.nextLine();
        }
        try {
            client.addSignature(noteId, reference);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void listMsg() {
        try {
            String delnote = client.getAsset(noteId);
            Document doc = XmlUtils.loadXML(delnote);
            ArrayList<String> messages = XmlUtils.getMessageList(doc);
            for (int i = 0; i < messages.size(); i++) {
                System.out.println(i + ". " + messages.get(i));
            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }
}
