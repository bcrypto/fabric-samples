import java.util.Scanner;

public class NoteMenu {
    private final String noteId;
    private final Client client;

     public NoteMenu(final Client client, final String noteId) {
        this.client = client;
        this.noteId = noteId;
    }

    public int start () {
        String name = client.getProperties().getProperty("note.action");
        if (name != null && name == "add") {
            addNewMsg();
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
                    addNewMsg();
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

    private void addNewMsg() {
    
        
        return;
    }

}
