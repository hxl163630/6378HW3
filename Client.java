import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Client {
    private static boolean successVote = false;
    private static int port;
    private static int VN = 0;
    private static int RU = 8;
    private static int DS = 0;
    private static int clientID;
    private static int level = 0;
    private static int serverNum = 8;
    private static int finishWrite = 0;
    private static String filePath = "/home/012/y/yx/yxm180012/cs6378/proj3/";
    private static ServerSocket ssocket;

    public static HashMap<Integer,Socket> socketMap = new HashMap<Integer,Socket>();
    public static HashMap<Socket,PrintWriter> outs = new HashMap<Socket,PrintWriter>();
    public static HashMap<Socket,BufferedReader> ins = new HashMap<Socket,BufferedReader>();

    //clientID, level, ReachableClient
    private static int[][][] components = new int[][][] {
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {0},{0}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {0,1,2,3}, {1,2,3}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {4,5,6}, {1,2,3,4,5,6}},
        {{0,1,2,3,4,5,6,7}, {4,5,6,7}, {7}, {7}}
    };

    public static HashMap<Integer, String> serverInfo = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1L;
        {
            put(0,"dc10.utdallas.edu");
            put(1,"dc11.utdallas.edu");
            put(2,"dc12.utdallas.edu");
            put(3,"dc13.utdallas.edu");
            put(4,"dc14.utdallas.edu");
            put(5,"dc15.utdallas.edu");
            put(6,"dc16.utdallas.edu");
            put(7,"dc17.utdallas.edu");
        }
    };

    public static Random rand = new Random();

    public Client(int clientID, int port) {
        this.clientID = clientID;
        this.port = port;

        // wait servers
        try {
            ssocket = new ServerSocket(port);
            socket = ssocket.accept();
            Thread.sleep(600);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initConnection();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //start threads to read sockets
        for (Socket socket : socketMap.values()) {
            Thread t = new Thread(new MsgHandler(socket));
            t.start();
        }

        // send completion notification after into cs 20 times
        while (level < 1)
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (finishWrite < 2) {
                if (startVote()) {
                    request2vote();
                }
            }
            if (successVote) {
                DS = components[clientID][level][0];
                RU = components[clientID][level].length;
                successVote = false;
            }

            finishWrite = 0;
            level++;
        }
    }

    private boolean startVote() {
        if (finishWrite >= 2) {
            return false;
        }
        if (level == 0 && clientID == 0) {
            return true;
        }
        if (level == 1 && (clientID == 0 || clientID == 4)) {
            return true;
        }
        if (level == 2 && (clientID == 0 || clientID == 1 || clientID == 4 || clientID == 7)) {
            return true;
        }
        if (level == 3 && clientID == 0 || clientID == 1|| clientID == 7) {
            return true;
        }
        return false;
    }

    private void request2vote() {
        int[] ClientsInComponent = components[clientID][level];

        String reqMsg;

        reqTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();

        reqMsg = "Write Level " + level + " Start Client " + clientID + " Other Cleint " + ClientsInComponent;

        for (int c: ClientsInComponent) {
            msgToClient(c, reqMsg);
        }

    }

    public static class MsgHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;

        public MsgHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public synchronized void run() {
            try {
                String inputLine;
                in = ins.get(socket);

                while ((inputLine = in.readLine()) != null) {
                    String[] splitStr;

                    System.out.println("Client " + clientID + " received msg: (" + inputLine + ") from  " + socket.getRemoteSocketAddress());
                    splitStr = inputLine.split("\\s+");

                    if ("Write".equals(splitStr[0])) {
                        int level = Integer.parseInt(splitStr[2]);
                        int fromClient = Integer.parseInt(splitStr[5]);
                        if (valideVote(fromClient, level)) {
                            VN ++;
                            successVote = true;
                            appendStrToFile(filePath.concat(Integer.toString(clientID)) + "/X.txt", inputLine);
                        }
                        finishWrite ++;
                    } else if ("END".equals(splitStr[0])) {
                        if (clientID == 1) {
                            for (PrintWriter outf : outs.values()) {
                                outf.println("END");
                                outf.flush();
                            }
                        }
                        stop();
                    } else {
                        System.out.println("Command Not Yet Support!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean valideVote(int level, int fromClient) {
        int[] ClientsInComponent = components[fromClient][level];

        if (ClientsInComponent.length * 2 == RU) {
            return Arrays.asList(ClientsInComponent).contains(DS);
        }
        if (ClientsInComponent.length * 2 > RU) {
            return true;
        }
        return false;
    }

    public synchronized static void msgToClient(int Client, String msg) {
        System.out.println("Send (" + msg + ") to server " + Client + " " + socketMap.get(Client).getRemoteSocketAddress());
        PrintWriter out = outs.get(socketMap.get(Client));
        out.println(msg);
        out.flush();
    }

    public static void appendStrToFile(String fileName, String str) {
        try {
            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(str);
            out.write("\n");
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("exception occoured" + e);
        }
    }

    public static void initConnection () {
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        try {
            for (int i = 1; i <= serverNum; i++) {
                if (i != clientID) {
                    socket = new Socket(serverInfo.get(i), port);
                    System.out.println("Client " + clientID + " connects to " + socket.getRemoteSocketAddress());
                    socketMap.put(i,socket);

                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outs.put(socket, out);
                    ins.put(socket, in);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // intput: components after partition
    public static void partition (int[] componentA, int[] componentB) {
        for (PrintWriter out : outs.values()) {
            out.close();
        }
    }

   // intput: components need to merge
    public static void merge (int[] componentA, int[] componentB) {

    }

    public static void stop() {

        System.out.println("Client " + clientID + " closing itself");
        try {
            for (BufferedReader in : ins.values()) {
                in.close();
            }
            for (PrintWriter out : outs.values()) {
                out.close();
            }
            for (Socket socket : socketMap.values()) {
                socket.close();
            }
            Thread.sleep(100);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // need user input of id
    // port(better be 49152 to 65535)
    public static void main(String[] args) throws IOException {

        // set basic param
        if (args.length > 0){
            try{
                // get start
                new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException e){
                System.err.println("Argument must be an integer");
                System.exit(1);
            }
        }
    }
}