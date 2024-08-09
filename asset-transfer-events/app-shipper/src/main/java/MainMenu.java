import java.util.Scanner;

public class MainMenu {
    private ChannelMenu menu;
    private final Client client;

    public MainMenu(final Client client) {
        this.client = client;
    }

    public void start() {
        String name = client.getProperties().getProperty("channel.name");
        if (name != null) {
            client.setChannel(name);
        } else if (show() == 1) {
            return;
        }
        System.out.println("Start working with " + client.getChannelName());
        menu = new ChannelMenu(client);
        while(menu.start() != 1) {
            int action = show();
            if (action == 1) {
                return;
            }
        }
    }

    public int show() {
        String prompt = "1. Exit\n2. Select current channel\n3. Show channel list";
        System.out.println("Current channel is: "+ client.getChannelName());
        System.out.println(prompt);
        Scanner in = new Scanner(System.in);
        int action = in.nextInt();
        while (action != 1) {
            switch(action) {
                case 2:
                    return 0;
                case 3:
                    chooseChannel(in);
                    break;
                default:
                    break;
            }
            System.out.println(prompt);
            action = in.nextInt();
        }
        return action;
    }

    private void chooseChannel(Scanner in) {
        var channels = client.getChannelList();
        System.out.println("Select channel:");
        for (int i=0; i < channels.size(); i++) {
            System.out.println(i + 1 + ". " + channels.get(i).getChannelId());
        }
        int choice = in.nextInt();
        if (choice > 0 && choice <= channels.size()) {
            String name = channels.get(choice - 1).getChannelId();
            if (name.equals(client.getChannelName())) {
                System.out.println("Channel " + name + " is already selected");
            } else {
                client.setChannel(name);
                System.out.println("Channel " + name + " is selected");
            }
        }
    }
}
