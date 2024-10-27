import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class assignment3 {
    private static String WORD = "happy";
    private static int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_CONNECTIONS = 10;
    private static final String FILENAME_FORMAT = "book_%02d.txt";

    static class ListNode {
        String line;
        ListNode nextNode;       // Link to the next element in the shared list
        ListNode bookNextNode;   // Link to the next item in the same book

        ListNode(String line) {
            this.line = line;
        }
    }

    static class SharedList {
        ListNode head;             // Head of the linked list for each book
        Lock lock;                 // Lock for thread-safe access
        int bookCounter;           // Track the book number

        SharedList() {
            this.head = null;
            this.bookCounter = 0;
            this.lock = new ReentrantLock();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final SharedList sharedList;

        ClientHandler(Socket clientSocket, SharedList sharedList) {
            this.clientSocket = clientSocket;
            this.sharedList = sharedList;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String line;
                int bookNumber;

                sharedList.lock.lock();
                try {
                    bookNumber = sharedList.bookCounter++;
                } finally {
                    sharedList.lock.unlock();
                }

                // Read data from the client
                while ((line = reader.readLine()) != null) {
                    addToList(sharedList, line, bookNumber);
                }

                // Write to file
                String filename = String.format(FILENAME_FORMAT, bookNumber);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                    ListNode currentNode = sharedList.head;
                    while (currentNode != null) {
                        writer.write(currentNode.line);
                        writer.newLine();
                        currentNode = currentNode.bookNextNode;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    

    private static void addToList(SharedList sharedList, String line, int bookNumber) {
        sharedList.lock.lock();
        try {
            ListNode newNode = new ListNode(line);

            if (sharedList.head == null) {
                sharedList.head = newNode;  // If it's the first node, set as head
            } else {
                ListNode currentNode = sharedList.head;
                while (currentNode.nextNode != null) {
                    currentNode = currentNode.nextNode;
                }
                currentNode.nextNode = newNode; // Add to the end of the list
            }

            // Update bookNextNode pointers
            if (bookNumber > 0) {
                ListNode currentNode = sharedList.head;
                while (currentNode != null && currentNode.bookNextNode != null) {
                    currentNode = currentNode.bookNextNode;
                }
                if (currentNode != null) {
                    currentNode.bookNextNode = newNode; // Link to the book's nodes
                }
            }

            System.out.println("Added: " + line);
        } finally {
            sharedList.lock.unlock();
        }
    }

    public static void prompt(String[] input) {
        Scanner scn = new Scanner(System.in);
        System.out.println("Input: ");

        String receievd = scn.nextLine();
        System.out.println("Entered: "+receievd);

    }

    public static void startServer(SharedList sharedList) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection from: " + clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket, sharedList)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for(int i=0 ; i<args.length; i++) {
            if(args[i].equals("-l")) {
                try {
                    PORT = Integer.parseInt(args[i+1]);
                } catch (Exception e) {
                    System.err.println("No port provided, using default port");
                }
            }

            if(args[i].equals("-s")) {
                try {
                    WORD = args[i+1];
                } catch (Exception e) {
                    System.err.println("No search string provided, using default search " + WORD);
                }
            }

        }
        

        SharedList sharedList = new SharedList();
        startServer(sharedList);
    }
}
