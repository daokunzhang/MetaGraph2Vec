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

public class PreProcessStep3 { // generate random walk sequences, including MetaGraph guided random walk, MetaPath guided random walk, and uniform random walk
	
	public static Map<String, Integer> author2Id;
	public static Map<String, Integer> venue2Id;
	public static Map<String, Integer> paper2Id;
	public static Map<Integer, String> Id2Author;
	public static Map<Integer, String> Id2Venue;
	public static Map<Integer, String> Id2Paper;
	
	public static ArrayList<ArrayList<Integer>> authorNeighbors;
	public static ArrayList<ArrayList<Integer>> venueNeighbors;
	public static ArrayList<ArrayList<Integer>> paperNeighborsAuthor;
	public static ArrayList<ArrayList<Integer>> paperNeighborsPaper;
	public static ArrayList<ArrayList<Integer>> paperNeighborsVenue;
	
	public static String metaGraphRandomWalk(int startNodeId, String type, int pathLen)
	{
		int curNodeId = startNodeId;
		String curType = type;
		int curPos = 1;
		Random random = new Random();
		StringBuffer path = new StringBuffer("");
		for(int i=0;i<pathLen;i++)
		{
			if(curType.equals("P"))
			{
				String paper = Id2Paper.get(curNodeId);
				if(i>0)
					path.append(" ");
				path.append(paper);
				if(curPos==2)
				{
					if(paperNeighborsAuthor.get(curNodeId).size()>1&&paperNeighborsVenue.get(curNodeId).size()>0) 
					{
						double option = Math.random();
						if(option<1.0/2.0)
						{
							int nextIndex = random.nextInt(paperNeighborsAuthor.get(curNodeId).size());
							curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
							curPos++;
							curType = "A";
						}
						else
						{
							int nextIndex = random.nextInt(paperNeighborsVenue.get(curNodeId).size());
							curNodeId = paperNeighborsVenue.get(curNodeId).get(nextIndex);
							curPos++;
							curType = "V";
						}
					}
					else if(paperNeighborsAuthor.get(curNodeId).size()>1)
					{
						int nextIndex = random.nextInt(paperNeighborsAuthor.get(curNodeId).size());
						curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
						curPos++;
						curType = "A";
					}
					else if(paperNeighborsVenue.get(curNodeId).size()>0)
					{
						int nextIndex = random.nextInt(paperNeighborsVenue.get(curNodeId).size());
						curNodeId = paperNeighborsVenue.get(curNodeId).get(nextIndex);
						curPos++;
						curType = "V";
					}
					else
						return path.toString();
				}
				else
				{
					if(paperNeighborsAuthor.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(paperNeighborsAuthor.get(curNodeId).size());
					curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
					curPos++;
					if(curPos>=5)
						curPos = 1;
					curType = "A";
				}
			}
			else
			{
				if(curType.equals("A"))
				{
					String author = Id2Author.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(author);
					if(authorNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(authorNeighbors.get(curNodeId).size());
					curNodeId = authorNeighbors.get(curNodeId).get(nextIndex);
					curPos++;
					curType = "P";
				}
				else if(curType.equals("V"))
				{
					String venue = Id2Venue.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(venue);
					if(venueNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(venueNeighbors.get(curNodeId).size());
					curNodeId = venueNeighbors.get(curNodeId).get(nextIndex);
					curPos++;
					curType = "P";
				}
			}
		}
		return path.toString();
	}
	
	public static String metaPathAPVPARandomWalk(int startNodeId, String type, int pathLen)
	{
		int curNodeId = startNodeId;
		String curType = type;
		String lastType = "";
		Random random = new Random();
		StringBuffer path = new StringBuffer("");
		for(int i=0;i<pathLen;i++)
		{
			if(curType.equals("P"))
			{
				String paper = Id2Paper.get(curNodeId);
				if(i>0)
					path.append(" ");
				path.append(paper);
				if(lastType.equals("A"))
				{
					if(paperNeighborsVenue.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(paperNeighborsVenue.get(curNodeId).size());
					curNodeId = paperNeighborsVenue.get(curNodeId).get(nextIndex);
					lastType = curType;
					curType = "V";
				}
				else
				{
					if(paperNeighborsAuthor.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(paperNeighborsAuthor.get(curNodeId).size());
					curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
					lastType = curType;
					curType = "A";
				}
			}
			else
			{
				if(curType.equals("A"))
				{
					String author = Id2Author.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(author);
					if(authorNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(authorNeighbors.get(curNodeId).size());
					curNodeId = authorNeighbors.get(curNodeId).get(nextIndex);
					lastType = curType;
					curType = "P";
				}
				else
				{
					String venue = Id2Venue.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(venue);
					if(venueNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(venueNeighbors.get(curNodeId).size());
					curNodeId = venueNeighbors.get(curNodeId).get(nextIndex);
					lastType = curType;
					curType = "P";
				}
			}
		}
		return path.toString();
	}
	
	public static String metaPathAPAPARandomWalk(int startNodeId, String type, int pathLen)
	{
		int curNodeId = startNodeId;
		String curType = type;
		Random random = new Random();
		StringBuffer path = new StringBuffer("");
		for(int i=0;i<pathLen;i++)
		{
			if(curType.equals("P"))
			{
				String paper = Id2Paper.get(curNodeId);
				if(i>0)
					path.append(" ");
				path.append(paper);
				if(paperNeighborsAuthor.get(curNodeId).size()<=1)
					return path.toString();
				int nextIndex = random.nextInt(paperNeighborsAuthor.get(curNodeId).size());
				curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
				curType = "A";
			}
			else
			{
				String author = Id2Author.get(curNodeId);
				if(i>0)
					path.append(" ");
				path.append(author);
				if(authorNeighbors.get(curNodeId).size()==0)
					return path.toString();
				int nextIndex = random.nextInt(authorNeighbors.get(curNodeId).size());
				curNodeId = authorNeighbors.get(curNodeId).get(nextIndex);
				curType = "P";
			}
		}
		return path.toString();
	}
	
	public static String uniformRandomWalk(int startNodeId, String type, int pathLen)
	{
		int curNodeId = startNodeId;
		String curType = type;
		Random random = new Random();
		StringBuffer path = new StringBuffer("");
		for(int i=0;i<pathLen;i++)
		{
			if(curType.equals("P"))
			{
				String paper = Id2Paper.get(curNodeId);
				if(i>0)
					path.append(" ");
				path.append(paper);
				int authorSize = paperNeighborsAuthor.get(curNodeId).size();
				int paperSize = paperNeighborsPaper.get(curNodeId).size();
				int venueSize = paperNeighborsVenue.get(curNodeId).size();
				if(authorSize+paperSize+venueSize==0)
					return path.toString();
				int nextIndex = random.nextInt(authorSize+paperSize+venueSize);
				if(nextIndex<authorSize)
				{
					curNodeId = paperNeighborsAuthor.get(curNodeId).get(nextIndex);
					curType = "A";
				}
				else if(nextIndex<authorSize+paperSize)
				{
					curNodeId = paperNeighborsPaper.get(curNodeId).get(nextIndex-authorSize);
					curType = "P";
				}
				else if(nextIndex<authorSize+paperSize+venueSize)
				{
					curNodeId = paperNeighborsVenue.get(curNodeId).get(nextIndex-authorSize-paperSize);
					curType = "V";
				}
			}
			else
			{
				if(curType.equals("A"))
				{
					String author = Id2Author.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(author);
					if(authorNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(authorNeighbors.get(curNodeId).size());
					curNodeId = authorNeighbors.get(curNodeId).get(nextIndex);
					curType = "P";
				}
				else if(curType.equals("V"))
				{
					String venue = Id2Venue.get(curNodeId);
					if(i>0)
						path.append(" ");
					path.append(venue);
					if(venueNeighbors.get(curNodeId).size()==0)
						return path.toString();
					int nextIndex = random.nextInt(venueNeighbors.get(curNodeId).size());
					curNodeId = venueNeighbors.get(curNodeId).get(nextIndex);
					curType = "P";
				}
			}
		}
		return path.toString();
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader brPaperAuthor = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.author.txt"), "UTF-8"));
		BufferedReader brPaperVenue = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.venue.txt"), "UTF-8"));
		BufferedReader brPaperPaper = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.paper.txt"), "UTF-8"));
		
		author2Id = new HashMap<String, Integer>();
		venue2Id = new HashMap<String, Integer>();
		paper2Id = new HashMap<String, Integer>();
		Id2Author = new HashMap<Integer, String>();
		Id2Venue = new HashMap<Integer, String>();
		Id2Paper = new HashMap<Integer, String>();
		
		String line = null;
		int paperIndex = 0, authorIndex = 0, venueIndex = 0;
		while((line=brPaperAuthor.readLine())!=null)
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
		brPaperAuthor.close();
		while((line=brPaperVenue.readLine())!=null)
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
		brPaperVenue.close();
		int numOfPaper = paperIndex;
		int numOfAuthor = authorIndex;
		int numOfVenue = venueIndex;
		
		authorNeighbors = new ArrayList<ArrayList<Integer>>();
		venueNeighbors = new ArrayList<ArrayList<Integer>>();
		paperNeighborsAuthor = new ArrayList<ArrayList<Integer>>();
		paperNeighborsPaper = new ArrayList<ArrayList<Integer>>();
		paperNeighborsVenue = new ArrayList<ArrayList<Integer>>();
		
		for(int i=0;i<numOfAuthor;i++)
		{
			ArrayList<Integer> neighbors = new ArrayList<Integer>();
			authorNeighbors.add(neighbors);
		}
		for(int i=0;i<numOfVenue;i++)
		{
			ArrayList<Integer> neighbors = new ArrayList<Integer>();
			venueNeighbors.add(neighbors);
		}
		for(int i=0;i<numOfPaper;i++)
		{
			ArrayList<Integer> neighborsAuthor = new ArrayList<Integer>();
			paperNeighborsAuthor.add(neighborsAuthor);
			ArrayList<Integer> neighborsPaper = new ArrayList<Integer>();
			paperNeighborsPaper.add(neighborsPaper);
			ArrayList<Integer> neighborsVenue = new ArrayList<Integer>();
			paperNeighborsVenue.add(neighborsVenue);
		}

		brPaperAuthor = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.author.txt"), "UTF-8"));
		brPaperVenue = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-V3.paper.venue.txt"), "UTF-8"));
		while((line=brPaperAuthor.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper = line.substring(0, splitPos);
			String author = line.substring(splitPos+1);
			int paperId = paper2Id.get(paper);
			int authorId = author2Id.get(author);
			if(!authorNeighbors.get(authorId).contains(paperId))
				authorNeighbors.get(authorId).add(paperId);
			if(!paperNeighborsAuthor.get(paperId).contains(authorId))
				paperNeighborsAuthor.get(paperId).add(authorId);
		}
		brPaperAuthor.close();
		while((line=brPaperPaper.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper1 = line.substring(0, splitPos);
			String paper2 = line.substring(splitPos+1);
			int paperId1 = paper2Id.get(paper1);
			int paperId2 = paper2Id.get(paper2);
			if(!paperNeighborsPaper.get(paperId1).contains(paperId2))
				paperNeighborsPaper.get(paperId1).add(paperId2);
			if(!paperNeighborsPaper.get(paperId2).contains(paperId1))
				paperNeighborsPaper.get(paperId2).add(paperId1);
		}
		brPaperPaper.close();
		while((line=brPaperVenue.readLine())!=null)
		{
			int splitPos = line.indexOf("\t");
			String paper = line.substring(0, splitPos);
			String venue = line.substring(splitPos+1);
			int paperId = paper2Id.get(paper);
			int venueId = venue2Id.get(venue);
			if(!venueNeighbors.get(venueId).contains(paperId))
				venueNeighbors.get(venueId).add(paperId);
			if(!paperNeighborsVenue.get(paperId).contains(venueId))
				paperNeighborsVenue.get(paperId).add(venueId);
		}
		brPaperVenue.close();
		
		int[] arrPaper1 = new int[numOfPaper];
		for(int i=0;i<numOfPaper;i++)
			arrPaper1[i] = i;
		Random randomPaper1 = new Random();
		for(int i=numOfPaper-1;i>0;i--) {
			int randNumPaper1 = randomPaper1.nextInt(i);
			int tempPaper1 = arrPaper1[i];
			arrPaper1[i] = arrPaper1[randNumPaper1];
			arrPaper1[randNumPaper1] = tempPaper1;
		}
		
		for(int i=0;i<numOfPaper;i++) {
			if(i%5==0) {
				int paperId = arrPaper1[i];
				for(int j=0;j<paperNeighborsVenue.get(paperId).size();j++) {
					int venueId = paperNeighborsVenue.get(paperId).get(j);
					int paperPos = -1;
					for(int k=0;k<venueNeighbors.get(venueId).size();k++) {
						if(venueNeighbors.get(venueId).get(k)==paperId) {
							paperPos = k;
							break;
						}
					}
					if(paperPos!=-1)
						venueNeighbors.get(venueId).remove(paperPos);
				}
				paperNeighborsVenue.get(paperId).clear();
			}
		}
		
		for(int i=0;i<numOfAuthor;i++)
		{
			Collections.sort(authorNeighbors.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
		}
		for(int i=0;i<numOfVenue;i++)
		{
			Collections.sort(venueNeighbors.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
		}
		
		for(int i=0;i<numOfPaper;i++)
		{
			Collections.sort(paperNeighborsAuthor.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
			Collections.sort(paperNeighborsPaper.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
			Collections.sort(paperNeighborsVenue.get(i), new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return a.compareTo(b);
				}
			});
		}
		
		BufferedWriter bwMetaGraphRandWalk = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomwalk"+File.separator+"DBLP-V3.meta.graph.random.walk.txt"), "UTF-8"));
		BufferedWriter bwMetaPathRandWalkAPVPA = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomwalk"+File.separator+"DBLP-V3.meta.path.random.walk.apvpa.txt"), "UTF-8"));
		BufferedWriter bwMetaPathRandWalkAPAPA = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomwalk"+File.separator+"DBLP-V3.meta.path.random.walk.apapa.txt"), "UTF-8"));
		BufferedWriter bwMetaPathRandWalkMix = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomwalk"+File.separator+"DBLP-V3.meta.path.random.walk.mix.txt"), "UTF-8"));
		BufferedWriter bwUniformRandWalk = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomwalk"+File.separator+"DBLP-V3.uniform.random.walk.txt"), "UTF-8"));
		int pathLen = 100;
		int numOfWalk = 80;
		for(int i=0;i<numOfWalk;i++)
		{
			System.out.println("rand walking iteration = "+i);
			int[] arr = new int[numOfAuthor]; // a random permutation of integers from 1 to numOfAuthor inclusive;
			for(int j=0;j<numOfAuthor;j++)
				arr[j] = j;
			Random random = new Random();
			for(int j=numOfAuthor-1;j>0;j--) {
				int randNum = random.nextInt(j);
				int temp = arr[j];
				arr[j] = arr[randNum];
				arr[randNum] = temp;
			}
			for(int j=0;j<numOfAuthor;j++)
			{
				String path = null;
				String type = "A";
				int startNodeId = arr[j];
				path = metaGraphRandomWalk(startNodeId, type, pathLen);
				bwMetaGraphRandWalk.write(path+"\n");
				path = metaPathAPVPARandomWalk(startNodeId, type, pathLen);
				bwMetaPathRandWalkAPVPA.write(path+"\n");
				path = metaPathAPAPARandomWalk(startNodeId, type, pathLen);
				bwMetaPathRandWalkAPAPA.write(path+"\n");
				path = uniformRandomWalk(startNodeId, type, pathLen);
				bwUniformRandWalk.write(path+"\n");
			}
		}
		int[] arr1 = new int[numOfWalk]; // a random permutation of integers from 1 to numOfWalk inclusive;
		for(int i=0;i<numOfWalk;i++)
			arr1[i] = i;
		Random random1 = new Random();
		for(int i=numOfWalk-1;i>0;i--) {
			int randNum1 = random1.nextInt(i);
			int temp1 = arr1[i];
			arr1[i] = arr1[randNum1];
			arr1[randNum1] = temp1;
		}
		for(int i=0;i<numOfWalk;i++)
		{
			System.out.println("rand walking iteration = "+i);
			int[] arr2 = new int[numOfAuthor]; // a random permutation of integers from 1 to numOfAuthor inclusive;
			for(int j=0;j<numOfAuthor;j++)
				arr2[j] = j;
			Random random2 = new Random();
			for(int j=numOfAuthor-1;j>0;j--) {
				int randNum2 = random2.nextInt(j);
				int temp2 = arr2[j];
				arr2[j] = arr2[randNum2];
				arr2[randNum2] = temp2;
			}
			for(int j=0;j<numOfAuthor;j++)
			{
				String path = null;
				String type = "A";
				int startNodeId = arr2[j];
				if(arr1[i]%2==0)
				{
					path = metaPathAPVPARandomWalk(startNodeId, type, pathLen);
					bwMetaPathRandWalkMix.write(path+"\n");
				}
				else
				{
					path = metaPathAPAPARandomWalk(startNodeId, type, pathLen);
					bwMetaPathRandWalkMix.write(path+"\n");
				}
			}
		}
		bwMetaGraphRandWalk.close();
		bwMetaPathRandWalkAPVPA.close();
		bwMetaPathRandWalkAPAPA.close();
		bwUniformRandWalk.close();
		bwMetaPathRandWalkMix.close();
		
		BufferedWriter bwNetwork = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.graph.txt"), "UTF-8"));
		for(int i=0;i<numOfPaper;i++)
		{
			int curNodeId = i;
			String curPaper = Id2Paper.get(curNodeId);
			for(int j=0;j<paperNeighborsPaper.get(curNodeId).size();j++)
			{
				int neighNodeId = paperNeighborsPaper.get(curNodeId).get(j);
				String neighPaper = Id2Paper.get(neighNodeId);
				bwNetwork.write(curPaper+" "+neighPaper+" 1\n");
			}
			for(int j=0;j<paperNeighborsAuthor.get(curNodeId).size();j++)
			{
				int neighNodeId = paperNeighborsAuthor.get(curNodeId).get(j);
				String neighAuthor = Id2Author.get(neighNodeId);
				bwNetwork.write(curPaper+" "+neighAuthor+" 1\n");
			}
			for(int j=0;j<paperNeighborsVenue.get(curNodeId).size();j++)
			{
				int neighNodeId = paperNeighborsVenue.get(curNodeId).get(j);
				String neighVenue = Id2Venue.get(neighNodeId);
				bwNetwork.write(curPaper+" "+neighVenue+" 1\n");
			}
		}
		for(int i=0;i<numOfAuthor;i++)
		{
			int curNodeId = i;
			String curAuthor = Id2Author.get(curNodeId);
			for(int j=0;j<authorNeighbors.get(curNodeId).size();j++)
			{
				int neighNodeId = authorNeighbors.get(curNodeId).get(j);
				String neighPaper = Id2Paper.get(neighNodeId);
				bwNetwork.write(curAuthor+" "+neighPaper+" 1\n");
			}
		}
		for(int i=0;i<numOfVenue;i++)
		{
			int curNodeId = i;
			String curVeune = Id2Venue.get(curNodeId);
			for(int j=0;j<venueNeighbors.get(curNodeId).size();j++)
			{
				int neighNodeId = venueNeighbors.get(curNodeId).get(j);
				String neighPaper = Id2Paper.get(neighNodeId);
				bwNetwork.write(curVeune+" "+neighPaper+" 1\n");
			}
		}
		bwNetwork.close();
		
		BufferedWriter bwLog = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.log.txt"), "UTF-8"));
		bwLog.write("numOfAuthor = "+numOfAuthor+"\n");
		bwLog.write("numOfPaper = "+numOfPaper+"\n");
		bwLog.write("numOfVenue = "+numOfVenue+"\n");
		int numOfPaperAuthorRelation = 0;
		int numOfPaperVenueRelation = 0;
		int numOfPaperPaperRelation = 0;
		for(int i=0;i<numOfPaper;i++)
		{
			numOfPaperAuthorRelation += paperNeighborsAuthor.get(i).size();
			numOfPaperPaperRelation += paperNeighborsPaper.get(i).size();
			numOfPaperVenueRelation += paperNeighborsVenue.get(i).size();
		}
		bwLog.write("numOfPaperAuthorRelation = "+numOfPaperAuthorRelation+"\n");
		bwLog.write("numOfPaperPaperRelation = "+numOfPaperPaperRelation+"\n");
		bwLog.write("numOfPaperVenueRelation = "+numOfPaperVenueRelation+"\n");
		bwLog.close();
  	}
}
