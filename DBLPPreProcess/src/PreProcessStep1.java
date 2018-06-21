import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class PreProcessStep1 { // extract subgraph form the DBLP-Citation-Network-V3.txt network
	
	public static int nonAlphabet(char ch)
	{
		if(ch>='A'&&ch<='Z')
			return 0;
		if(ch>='a'&&ch<='z')
			return 0;
		return 1;
	}

	public static int check(String venue)
	{
		String[] selectedVenues = {"SIGMOD", "ICDE", "VLDB", "EDBT",
				"PODS", "ICDT", "DASFAA", "SSDBM", "CIKM", "KDD", 
				"ICDM", "SDM", "PKDD", "PAKDD", "IJCAI", "AAAI", 
				"NIPS", "ICML", "ECML", "ACML", "IJCNN", "UAI",
				"ECAI", "COLT", "ACL", "KR", "CVPR", "ICCV",
				"ECCV", "ACCV", "MM", "ICPR", "ICIP", "ICME"};
		int res = 0;
		for(int i=0;i<selectedVenues.length;i++)
		{
			int index = venue.indexOf(selectedVenues[i]);
			if(index!=-1)
			{
				if(index==0)
				{
					if(index+selectedVenues[i].length()>=venue.length())
						res = 1;
					else
					{
						char endChar = venue.charAt(index+selectedVenues[i].length());
						if(nonAlphabet(endChar)==1)
							res = 1;
					}
				}
				else
				{
					char startChar = venue.charAt(index-1);
					if(nonAlphabet(startChar)==1)
					{
						if(index+selectedVenues[i].length()>=venue.length())
							res = 1;
						else
						{
							char endChar = venue.charAt(index+selectedVenues[i].length());
							if(nonAlphabet(endChar)==1)
								res = 1;
						}
					}
				}
			}
			if(res==1)
				break;
		}
		if(res==1)
		{
			if(venue.indexOf("SIGMOD")!=-1&&venue.indexOf("Data Mining")!=-1)
				res = 0;
		}
		return res;
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("dataset"+File.separator+"DBLP-Citation-Network-V3.txt"), "UTF-8"));
		BufferedWriter bwTitle = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.paper.title.txt"), "UTF-8"));
		BufferedWriter bwAuthor = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.paper.author.txt"), "UTF-8"));
		BufferedWriter bwVenue = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.paper.venue.txt"), "UTF-8"));
		BufferedWriter bwRef = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dataset"+File.separator+"DBLP-V3.paper.ref.txt"), "UTF-8"));
		int numOfNode = 0;
		String line = null;
		br.readLine();
		String curIndex = "";
		String curTitle = "";
		String curAuthors = "";
		String[] curAuthorList = null;
		String curVenue = "";
		ArrayList<String> curRef = new ArrayList<String>();
		int writeStatus = 0;
		int maxRefSize = 0;
		String maxRefIndex = "";
		while((line=br.readLine())!=null)
		{
			if(line.indexOf("#*")==0)
				curTitle = line.substring(2).trim();
			if(line.indexOf("#c")==0)
			{
				curVenue = line.substring(2).trim();
				curVenue = curVenue.replaceAll(" ", "");
				if(check(curVenue)==1)
					writeStatus = 1;
				else
					writeStatus = 0;
			}
			if(line.indexOf("#%")==0)
				curRef.add(line.substring(2).trim());
			if(line.indexOf("#@")==0)
			{
				curAuthors = line.substring(2).trim();
				curAuthorList = curAuthors.split(",");
			}
			if(line.indexOf("#index")==0) 
				curIndex = line.substring(6).trim();
			if(line.trim().length()==0)
			{
				if((!curTitle.equals(""))&&(!curVenue.equals(""))&&(!curAuthors.equals(""))&&(!curIndex.equals(""))&&writeStatus==1)
				{
					curIndex = curIndex.replaceAll(" ", "");
					bwTitle.write("p_"+curIndex+"\t"+curTitle+"\n");
					for(int i=0;i<curAuthorList.length;i++) {
						String curAuthor = curAuthorList[i];
						curAuthor = curAuthor.replaceAll(" ", "");
						if(!curAuthor.equals(""))
							bwAuthor.write("p_"+curIndex+"\ta_"+curAuthor+"\n");
					}
					bwVenue.write("p_"+curIndex+"\tv_"+curVenue+"\n");
					//bwRef.write("p_"+curIndex+"\tsize:\t"+curRef.size()+"\n");
					for(int i=0;i<curRef.size();i++)
					{
						String curReference = curRef.get(i);
						curReference = curReference.replaceAll(" ", "");
						if(!curReference.equals(""))
							bwRef.write("p_"+curIndex+"\tp_"+curReference+"\n");
					}
					if(curRef.size()>maxRefSize)
					{
						maxRefSize = curRef.size();
						maxRefIndex = curIndex;
					}
					numOfNode++;
				}
				curTitle = "";
				curAuthors = "";
				curVenue = "";
				curRef.clear();
				curIndex = "";
			}
		}
		br.close();
		bwTitle.close();
		bwAuthor.close();
		bwVenue.close();
		bwRef.close();
		System.out.println("numOfNode = "+numOfNode);
		System.out.println("maxRefSize = "+maxRefSize);
		System.out.println("maxRefIndex = "+maxRefIndex);
	}

}
