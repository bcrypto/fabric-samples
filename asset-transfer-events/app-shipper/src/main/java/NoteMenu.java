import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.w3c.dom.Document;

public class NoteMenu {
    private final String noteId;
    private final Client client;

     public NoteMenu(final Client client, final String noteId) {
        this.client = client;
        this.noteId = noteId;
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
        String prompt = "0. Back to menu\n1. Exit\n2. Add message\n3. Export message";
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

}
