import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Server implements Runnable {
    private static boolean successVote = false;
    private static int port;
    private static int writeTime = 0;
    private static int VN = 1;
    private static int RU = 8;
    private static int DS = 0;
    private static int serverID;
    private static int level = 0;
    private static int serverNum = 8;
    private static int finishWrite = 0;
    private static ServerSocket ssocket;
    public static HashMap<Integer,Socket> socketMap = new HashMap<Integer,Socket>();
    public static HashMap<Socket,PrintWriter> outs = new HashMap<Socket,PrintWriter>();
    public static HashMap<Socket,BufferedReader> ins = new HashMap<Socket,BufferedReader>();
    public static Scanner keyboard;
    public static HashMap<Character, Integer> nameMap = new HashMap<Character, Integer>() {
        private static final long serialVersionUID = 1L;
        {
            put('A',0);
            put('B',1);
            put('C',2);
            put('D',3);
            put('E',4);
            put('F',5);
            put('G',6);
            put('H',7);
        }
    };

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

    // create server log file
    public void createFile() {
        File ClientFile = new File(getKey(nameMap,serverID) + ".txt");
        File LogFile = new File(getKey(nameMap,serverID) + "_Log.txt");
        if (!ClientFile.exists() && !LogFile.exists()) {
            try {
                LogFile.createNewFile();
                ClientFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                System.err.println("[ERROR]: Cannot create ServerFile!");
            }
        }
    }

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

    public Server(int serverID, int port) {
        this.serverID = serverID;
        this.port = port;

        // create files
        createFile();
        // wait servers
        initConnection();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
       //start threads to read sockets
        for (Socket socket : socketMap.values()) {
            Thread mh = new Thread(new MsgHandler(socket));
            mh.start();
        }
        keyboard = new Scanner(System.in);

        while (level < 4)
        {
            System.out.println("*********************** Level:" + level + " ***********************");
            while(finishWrite < 2){
                if (startVote()) {
                    request2vote();
                    // sleep after start a vote
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    validateWrite(serverID, level, "Write Level " + level + " StartServer " + serverID + " current VN " + VN, VN);
                }
                System.out.print("");
            }

            if (successVote) {
                DS = components[serverID][level][0];
                RU = components[serverID][level].length;
                successVote = false;
            }

            finishWrite = 0;
            level++;

            while (!(keyboard.next()).equals("n")) {
                System.out.println("Not match, please type in again.");
            }
        }
        stop();
    }

    public static class MsgHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;

        public MsgHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                in = ins.get(socket);

                //msg: "Write Level X StartServer X"
                while ((inputLine = in.readLine()) != null) {
                    String[] splitStr;

                    //System.out.println("Received msg: (" + inputLine + ") from  " + socket.getRemoteSocketAddress());
                    splitStr = inputLine.split("\\s+");

                    if ("Write".equals(splitStr[0])) {
                        int level = Integer.parseInt(splitStr[2]);
                        int fromServer = Integer.parseInt(splitStr[4]);
                        validateWrite(fromServer, level, inputLine, Integer.parseInt(splitStr[splitStr.length - 1]));
                    } else {
                        System.out.println("Command Not Yet Support!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean startVote() {
        if (finishWrite >= 2) {
            return false;
        }
        if (level == 0 && serverID == 0) {
            return true;
        }
        if (level == 1 && (serverID == 0 || serverID == 4)) {
            return true;
        }
        if (level == 2 && (serverID == 0 || serverID == 1 || serverID == 4 || serverID == 7)) {
            return true;
        }
        if (level == 3 && (serverID == 0 || serverID == 1 || serverID == 7)) {
            return true;
        }
        return false;
    }

    private void request2vote() {
        int[] ServersInComponent = components[serverID][level];
        String reqMsg;

        //msg: "Write Level X StartServer X"
        reqMsg = "Write Level " + level + " StartServer " + serverID + " current VN " + VN;

        for (int c: ServersInComponent) {
            if (c == serverID) {
                continue;
            }
            msgToServer(c, reqMsg);
        }
    }

    public static boolean validateVote(int fromServer, int level) {
        int[] ServersInComponent = components[fromServer][level];
        if (ServersInComponent.length * 2 == RU) {
            for(int i: ServersInComponent)  {
                if (i == DS) {
                    return true;
                }
            }
        }
        if (ServersInComponent.length * 2 > RU) {
            return true;
        }
        return false;
    }

    public static void validateWrite(int fromServer, int level, String inputLine, int fromVN) {
        if (validateVote(fromServer, level)) {
            compareVN(fromServer, fromVN);
            VN ++;
            successVote = true;
            appendStrToFile(getKey(nameMap,serverID) + ".txt", inputLine);
            System.out.println(inputLine + " RU " + RU + " DS " + DS);
        }
        writeTime ++;
        writeLog();
        finishWrite ++;
    }

    public static void writeLog() {
        String line = "******************* Write Attempt " + writeTime + " *******************";
        appendStrToFile(getKey(nameMap,serverID) + "_Log.txt", line);
        try (BufferedReader br = new BufferedReader(new FileReader(getKey(nameMap,serverID) + ".txt"))) {
            while ((line = br.readLine()) != null) {
               // process the line.
                appendStrToFile(getKey(nameMap,serverID) + "_Log.txt", line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compareVN(int fromServer, int fromVN) {
        if (fromVN > VN) {
            try {
                VN = fromVN;
                Files.copy(Paths.get(getKey(nameMap,fromServer) + ".txt"), Paths.get(getKey(nameMap,serverID) + ".txt"), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void msgToServer(int Server, String msg) {
        //System.out.println("Send (" + msg + ") to server" + socketMap.get(Server).getRemoteSocketAddress());
        PrintWriter out = outs.get(socketMap.get(Server));
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
        int i,j;
        Socket socket;
        PrintWriter out;
        BufferedReader in;

        try {
            for (i = serverNum; i > 1; i--) { // i: [8,2]
                j = serverNum - i;  // j: [0,6]
                if (serverID > j) {
                    Thread.sleep(serverID*50);
                    System.out.println("Server " + getKey(nameMap,serverID) + " starts clientSocket " + serverInfo.get(j) + " port " + (port + serverID - 1));
                    socket = new Socket(serverInfo.get(j), port + serverID - 1);
                    socketMap.put(j,socket);
                } else {
                    System.out.println("Server " + getKey(nameMap,serverID) + " starts serverSocket port " + (port + j));
                    ssocket = new ServerSocket(port + j);
                    socket = ssocket.accept();
                    socketMap.put(j+1,socket);
                }

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outs.put(socket, out);
                ins.put(socket, in);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // intput: components after partition
    public static void partition (int[] componentA, int[] componentB) {
        if (contains(componentA,serverID)) {
            for (int server : componentB) {
                try {
                    outs.get(socketMap.get(server)).close();
                    outs.remove(socketMap.get(server));
                    ins.get(socketMap.get(server)).close();
                    ins.remove(socketMap.get(server));
                    socketMap.get(server).close();
                    socketMap.remove(server);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

   // intput: components need to merge
    public static void merge (int[] componentA, int[] componentB) {
        if (contains(componentA,serverID)) {
            for (int server : componentB) {
                try {
                    Socket socket = new Socket(serverInfo.get(server),port);
                    System.out.println("Server " + getKey(nameMap,serverID) + " connects to " + socket.getRemoteSocketAddress());
                    socketMap.put(server,socket);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    outs.put(socket, out);
                    ins.put(socket, in);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean contains(final int[] array, final int v) {
        boolean result = false;
        for(int i : array){
            if(i == v){
                result = true;
                break;
            }
        }
        return result;
    }

    public static void stop() {
        System.out.println("Server " + getKey(nameMap,serverID) + " closing itself");
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
            keyboard.close();
            Thread.sleep(100);
            System.exit(0);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (K key : map.keySet()) {
            if (value.equals(map.get(key))) {
                return key;
            }
        }
        return null;
    }

    // need user input of id
    // port(better be 49152 to 65535)
    public static void main(String[] args) throws IOException {

        // set basic param
        if (args.length > 0){
            try{
                // get start
                Server s = new Server(nameMap.get(args[0].charAt(0)), Integer.parseInt(args[1]));
                Thread st = new Thread(s);
                st.start();
            } catch (NumberFormatException e){
                System.err.println("Argument must be an integer");
                System.exit(1);
            }
        }
    }


}