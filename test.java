import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.Timestamp;

public class test{
	private HashMap<Integer, int[][]> components;
	private int VN;
	private int RU;
	private int DS;
	private test() {
		// key for components HashMap is the client id
		// value for components HashMap is a 2d array, store the clients id within each component in each level
		this.components.put(0, new int[][] {{1,2,3,4,5,6,7},
											{1,2,3},
											{},
											{}});
		this.components.put(1, new int[][] {{0,2,3,4,5,6,7},
											{0,2,3},
											{2,3},
											{2,3,4,5,6}});
		this.components.put(2, new int[][] {{0,1,3,4,5,6,7},
											{0,1,3},
											{1,3},
											{1,3,4,5,6}});
		this.components.put(3, new int[][] {{0,1,2,4,5,6,7},
											{0,1,2},
											{1,2},
											{1,2,4,5,6}});
		this.components.put(4, new int[][] {{0,1,2,3,5,6,7},
											{5,6,7},
											{5,6},
											{1,2,3,5,6}});
		this.components.put(5, new int[][] {{0,1,2,3,4,6,7},
											{4,6,7},
											{4,6},
											{1,2,3,4,6}});
		this.components.put(6, new int[][] {{0,1,2,3,4,5,7},
											{4,5,7},
											{4,5},
											{1,2,3,4,5}});
		this.components.put(7, new int[][] {{0,1,2,3,4,5,6},
											{4,5,6},
											{},
											{}});
	}
	
	private void vote(int level, int clientID, HashMap<Integer, int[][]> components) {
		int[] otherClients = components.get(clientID)[level];
		
	}
}
