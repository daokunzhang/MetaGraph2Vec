import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PreProcessStep2 { // obtain the class labels for authors;
	
	public static int getClass(String venue)
	{
		String[] venues1 = {"SIGMOD", "ICDE", "VLDB", "EDBT",
				"PODS", "ICDT", "DASFAA", "SSDBM", "CIKM"};
		String[] venues2 = {"KDD", "ICDM", "SDM", "PKDD", "PAKDD"};
		String[] venues3 = {"IJCAI", "AAAI", "NIPS", "ICML", "ECML", 
				"ACML", "IJCNN", "UAI", "ECAI", "COLT", "ACL", "KR"};
		String[] venues4 = {"CVPR", "ICCV", "ECCV", "ACCV", "MM", 
				"ICPR", "ICIP", "ICME"};
		ArrayList<ArrayList<String>> venueLists = new ArrayList<ArrayList<String>>();
		ArrayList<String> venueList1 = new ArrayList<String>();
		for(int i=0;i<venues1.length;i++)
			venueList1.add(venues1[i]);
		venueLists.add(venueList1);
		ArrayList<String> venueList2 = new ArrayList<String>();
		for(int i=0;i<venues2.length;i++)
			venueList2.add(venues2[i]);
		venueLists.add(venueList2);
		ArrayList<String> venueList3 = new ArrayList<String>();
		for(int i=0;i<venues3.length;i++)
			venueList3.add(venues3[i]);
		venueLists.add(venueList3);
		ArrayList<String> venueList4 = new ArrayList<String>();
		for(int i=0;i<venues4.length;i++)
			venueList4.add(venues4[i]);
		venueLists.add(venueList4);
		
		int[] arrClass = new int[4]; // a random permutation of integers from 0 to 3 inclusive;
		for(int i=0;i<4;i++)
			arrClass[i] = i;
		Random randomClass = new Random();
		for(int i=3;i>0;i--) 
		{
			int randNumClass = randomClass.nextInt(i);
			int temp = arrClass[i];
			arrClass[i] = arrClass[randNumClass];
			arrClass[randNumClass] = temp;
		}
		for(int i=0;i<4;i++)
		{
			for(int j=0;j<venueLists.get(arrClass[i]).size();j++)
			{
				String venueToken = venueLists.get(arrClass[i]).get(j);
				if(venue.indexOf(venueToken)!=-1)
					return arrClass[i];
			}
		}
		return 0;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader brAuthor = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.author.txt"), "UTF-8"));
		BufferedReader brVenue = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.venue.txt"), "UTF-8"));
		BufferedReader brRef = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.ref.txt"), "UTF-8"));
		Map<String, Integer> author2Id = new HashMap<String, Integer>();
		Map<String, Integer> venue2Id = new HashMap<String, Integer>();
		Map<String, Integer> paper2Id = new HashMap<String, Integer>();
		Map<Integer, String> Id2Author = new HashMap<Integer, String>();
		Map<Integer, String> Id2Venue = new HashMap<Integer, String>();
		Map<Integer, String> Id2Paper = new HashMap<Integer, String>();
		
		String line = null;
		int paperIndex = 0, authorIndex = 0, venueIndex = 0;
		while((line=brAuthor.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper = line.substring(0, splitPos);
			String author = line.substring(splitPos+1);
			if(!paper2Id.containsKey(paper))
			{
				paper2Id.put(paper, paperIndex);
				Id2Paper.put(paperIndex, paper);
				paperIndex++;
			}
			if(!author2Id.containsKey(author))
			{
				author2Id.put(author, authorIndex);
				Id2Author.put(authorIndex, author);
				authorIndex++;
			}
		}
		brAuthor.close();
		while((line=brVenue.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String venue = line.substring(splitPos+1);
			if(!venue2Id.containsKey(venue))
			{
				venue2Id.put(venue, venueIndex);
				Id2Venue.put(venueIndex, venue);
				venueIndex++;
			}
		}
		brVenue.close();
		int numOfPaper = paperIndex;
		int numOfAuthor = authorIndex;
		int numOfVenue = venueIndex;

		BufferedWriter bwPaperPaper = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.paper.paper.txt"), "UTF-8"));
		while((line=brRef.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper1 = line.substring(0, splitPos);
			String paper2 = line.substring(splitPos+1);
			if(paper2Id.containsKey(paper2))
				bwPaperPaper.write(paper1+"\t"+paper2+"\n");
		}
		brRef.close();
		bwPaperPaper.close();
		
		ArrayList<ArrayList<Integer>> authorNeighbors = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> paperNeighborsVenue = new ArrayList<ArrayList<Integer>>();
		for(int i=0;i<numOfAuthor;i++)
		{
			ArrayList<Integer> neighbors = new ArrayList<Integer>();
			authorNeighbors.add(neighbors);
		}
		for(int i=0;i<numOfPaper;i++)
		{
			ArrayList<Integer> neighborsVenue = new ArrayList<Integer>();
			paperNeighborsVenue.add(neighborsVenue);
		}
		brAuthor = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.author.txt"), "UTF-8"));
		brVenue = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.venue.txt"), "UTF-8"));
		while((line=brAuthor.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper = line.substring(0, splitPos);
			String author = line.substring(splitPos+1);
			int paperId = paper2Id.get(paper);
			int authorId = author2Id.get(author);
			if(!authorNeighbors.get(authorId).contains(paperId))
				authorNeighbors.get(authorId).add(paperId);
		}
		brAuthor.close();
		while((line=brVenue.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper = line.substring(0, splitPos);
			String venue = line.substring(splitPos+1);
			int paperId = paper2Id.get(paper);
			int venueId = venue2Id.get(venue);
			if(!paperNeighborsVenue.get(paperId).contains(venueId))
				paperNeighborsVenue.get(paperId).add(venueId);
		}
		brVenue.close();
		for(int i=0;i<numOfAuthor;i++)
		{
			Collections.sort(authorNeighbors.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
		}
		for(int i=0;i<numOfPaper;i++)
		{
			Collections.sort(paperNeighborsVenue.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
		}
		
		int[][] venueClass = new int[numOfVenue][4];
		int[][] authorClass = new int[numOfAuthor][4];
		for(int i=0;i<numOfVenue;i++)
		{
			for(int j=0;j<4;j++)
				venueClass[i][j] = 0;
			String venue = Id2Venue.get(i);
			int label = getClass(venue);
			venueClass[i][label] = 1;
		}
		for(int i=0;i<numOfAuthor;i++)
		{
			for(int j=0;j<4;j++)
				authorClass[i][j] = 0;
			for(int j=0;j<authorNeighbors.get(i).size();j++)
			{
				int paperId = authorNeighbors.get(i).get(j);
				for(int k=0;k<paperNeighborsVenue.get(paperId).size();k++)
				{
					int venueId = paperNeighborsVenue.get(paperId).get(k);
					String venue = Id2Venue.get(venueId);
					int label = getClass(venue);
					authorClass[i][label] += 1;
				}
			}
		}
		for(int i=0;i<numOfAuthor;i++)
		{
			int[] arrClass = new int[4]; // a random permutation of integers from 0 to 3 inclusive;
			for(int j=0;j<4;j++)
				arrClass[j] = j;
			Random randomClass = new Random();
			for(int j=3;j>0;j--) 
			{
				int randNumClass = randomClass.nextInt(j);
				int temp = arrClass[j];
				arrClass[j] = arrClass[randNumClass];
				arrClass[randNumClass] = temp;
			}
			int label = -1;
			int maxNum = -1;
			for(int j=0;j<4;j++)
			{
				if(authorClass[i][arrClass[j]]>maxNum)
				{
					maxNum = authorClass[i][arrClass[j]];
					label = arrClass[j];
				}
			}
			if(label==-1)
				System.out.println("no class label for author "+i);
			else
			{
				for(int j=0;j<4;j++)
					authorClass[i][j] = 0;
				authorClass[i][label] = 1;
			}
		}
		
		BufferedWriter bwAuthorClass = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("group"+File.separator+"DBLP-V3.author.class.txt"), "UTF-8"));
		for(int i=0;i<numOfAuthor;i++)
		{
			String author = Id2Author.get(i);
			bwAuthorClass.write(author);
			for(int j=0;j<4;j++)
				bwAuthorClass.write(" "+authorClass[i][j]);
			bwAuthorClass.write("\n");
		}
		bwAuthorClass.close();
  	}
}
