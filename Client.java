import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.Timestamp;

public class Client {
    private static boolean reqFlag = false;
    private static int clientID;
    private static int port;
    private static int reqCount = 0;
    private static int serverNum = 7;
    private static int repliedCount = 0;
    private static int msgSentCS, msgSentTotal = 0;
    private static int msgRecivedTotal = 0;
    private static long reqTimestamp=0L;
    private static long csTimestamp=0L;
    private static long resTimestamp=0L;
    private static final String file = "/home/012/y/yx/yxm180012/cs6378/proj2/treeBaseQuorumSystem.txt";
    private static ArrayList<Integer> quorum;

    public static HashMap<Integer,Socket> socketMap = new HashMap<Integer,Socket>();
    public static HashMap<Socket,PrintWriter> outs = new HashMap<Socket,PrintWriter>();
    public static HashMap<Socket,BufferedReader> ins = new HashMap<Socket,BufferedReader>();

    public static ArrayList<ArrayList<Integer>> coterie = new ArrayList<ArrayList<Integer>> () {
        {
            add(new ArrayList<Integer>(Arrays.asList(1,2,4)));
            add(new ArrayList<Integer>(Arrays.asList(1,2,5)));
            add(new ArrayList<Integer>(Arrays.asList(1,3,6)));
            add(new ArrayList<Integer>(Arrays.asList(1,3,7)));
            add(new ArrayList<Integer>(Arrays.asList(2,3,4,6)));
            add(new ArrayList<Integer>(Arrays.asList(2,3,4,7)));
            add(new ArrayList<Integer>(Arrays.asList(2,3,5,6)));
            add(new ArrayList<Integer>(Arrays.asList(2,3,5,7)));
            add(new ArrayList<Integer>(Arrays.asList(1,4,5)));
            add(new ArrayList<Integer>(Arrays.asList(1,6,7)));
            add(new ArrayList<Integer>(Arrays.asList(3,4,5,6)));
            add(new ArrayList<Integer>(Arrays.asList(3,4,5,7)));
            add(new ArrayList<Integer>(Arrays.asList(2,4,6,7)));
            add(new ArrayList<Integer>(Arrays.asList(2,5,6,7)));
            add(new ArrayList<Integer>(Arrays.asList(4,5,6,7)));
        }
    };

    public static HashMap<Integer, String> serverInfo = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1L;
        {
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
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        buildConnection();
        try {
            Thread.sleep(600+rand.nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //start threads to read sockets
        for (Socket socket : socketMap.values()) {
            Thread t = new Thread(new MsgHandler(socket));
            t.start();
        }

        // send completion notification after into cs 20 times
        while (reqCount < 20)
        {
            try {
                Thread.sleep(rand.nextInt(6)+5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!reqFlag) {
                request2CS();
            }
        }
        msgToServer(1, "COMPLETION");
    }

    public static class MsgHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;

        public MsgHandler(Socket socket) {
            this.socket = socket;
        }

        public synchronized void run() {
            try {
                String inputLine;
                in = ins.get(socket);

                while ((inputLine = in.readLine()) != null) {
                    String[] splitStr;

                    System.out.println("Client " + clientID + " received msg: (" + inputLine + ") from  " + socket.getRemoteSocketAddress());
                    splitStr = inputLine.split("\\s+");

                    if ("GRANT".equals(splitStr[0])) {
                        // into cs
                        if (++repliedCount == quorum.size()) {
                            criticalSection();
                            releaseCS();
                            msgRecivedTotal += repliedCount;
                            msgSentTotal += msgSentCS;
                            reqFlag = false;
                        }
                    } else if ("END".equals(splitStr[0])) {
                        if (clientID == 1) {
                            for (PrintWriter outf : outs.values()) {
                                outf.println("END");
                                outf.flush();
                            }
                        }
                        System.out.println("###  total number of messages sent: " + msgSentTotal + ", total number of messages received: " + msgRecivedTotal);
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

    public synchronized static void msgToServer(int server, String msg) {
        msgSentCS++;
        System.out.println("Send (" + msg + ") to server " + server + " " + socketMap.get(server).getRemoteSocketAddress());
        PrintWriter out = outs.get(socketMap.get(server));
        out.println(msg);
        out.flush();
    }

    public static void request2CS() {
        String reqMsg;
        reqFlag = true;
        msgSentCS = 0;
        repliedCount = 0;

        reqTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();
        if (resTimestamp != 0L) {
            System.out.println("### time between a client exiting its CS and issuing its next request: " + (reqTimestamp - resTimestamp));
        }

        // prepare message and send it to its quorum
        System.out.println("****** " + "reqCount: " + reqCount + " ******");
        reqCount++;
        quorum = coterie.get(rand.nextInt(15));
        System.out.println("quorum " + quorum);
        reqMsg = "REQUEST " + clientID + " " + reqTimestamp;
        for (Integer serverNum : quorum) {
            msgToServer(serverNum, reqMsg);
        }
    }

    public static void releaseCS() {
        for (Integer serverNum : quorum) {
            msgToServer(serverNum, "RELEASE " + clientID);
        }
    }

    public static void appendStrToFile(String fileName, String str) {
        try {
            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(str);
            out.write("\n");
            out.flush();

            Thread.sleep(3);
            out.close();
        } catch (Exception e) {
            System.out.println("exception occoured" + e);
        }
    }

    public static void criticalSection() {
        csTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();
        System.out.println("### number of messages exchanged: " + (repliedCount + msgSentCS) + ", elapsed time: " + (csTimestamp - reqTimestamp));
        String str = "Entering - clientId: " + clientID + ", timestamp: " + reqTimestamp;
        appendStrToFile(file, str);
        resTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();
        System.out.println("### time spent in the CS: " + (resTimestamp - csTimestamp));
    }

    public static void buildConnection () {
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        try {
            for (int i = 1; i <= serverNum; i++) {
                socket = new Socket(serverInfo.get(i), port);
                System.out.println("Client " + clientID + " connects to " + socket.getRemoteSocketAddress());
                socketMap.put(i,socket);

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outs.put(socket, out);
                ins.put(socket, in);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
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