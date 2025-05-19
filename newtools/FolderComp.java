package newtools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class FolderComp {
	private static String REF_FOLDER = "";
	private static String FOLDER2CLEAN = "";
	private static String COMPCLASS = "";

	private static boolean DELETE_SAME_FILES = false;

	private static Set<String> EXCLUDED_FORMATS_LIST = new HashSet<>();
	private static Map<String, List> folderMap = new HashMap<>();

	private static List<String> deletableFiles = new ArrayList<>();
	private static List<String> unmatchedFiles = new ArrayList<>();
	private static List<String> comparedFiles = new ArrayList<>();
	private static ResourceBundle rb = null;
	static {
		EXCLUDED_FORMATS_LIST.add("xml");

		try {
			rb = ResourceBundle.getBundle("comparison");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		COMPCLASS = rb.getString("COMPCLASS");
		if ("1".equals(COMPCLASS)) {
			REF_FOLDER = rb.getString("REF_FOLDER");
			FOLDER2CLEAN = rb.getString("FOLDER2CLEAN");
		}
		if ("2".equals(COMPCLASS)) {
			REF_FOLDER = rb.getString("REF_FOLDER2");
			FOLDER2CLEAN = rb.getString("FOLDER2CLEAN2");
		}
		if ("3".equals(COMPCLASS)) {
			REF_FOLDER = rb.getString("REF_FOLDER3");
			FOLDER2CLEAN = rb.getString("FOLDER2CLEAN3");
		}
		String toDelete = rb.getString("DELETE_SAME_FILES");
		DELETE_SAME_FILES = "true".equals(toDelete);

		String excludedFiles = rb.getString("EXCLUDED_FORMATS_LIST");
		String[] arr = excludedFiles.split(",");
		EXCLUDED_FORMATS_LIST = new HashSet<>(Arrays.asList(arr));
	}

	public static void main(String[] args) throws IOException {

		File root = new File(REF_FOLDER);
		if (args.length <= 0) {
			// print
		} else {
			COMPCLASS = args[0];
			if ("1".equals(COMPCLASS)) {
				REF_FOLDER = rb.getString("REF_FOLDER");
				FOLDER2CLEAN = rb.getString("FOLDER2CLEAN");
			} else {
				REF_FOLDER = rb.getString("REF_FOLDER2");
				FOLDER2CLEAN = rb.getString("FOLDER2CLEAN2");
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String userInput = br.readLine();
			if (!"y".equalsIgnoreCase(userInput)) {
				System.out.println("Exiting..you entered " + userInput);
				return;
			}
		}

		getList(root);// prepare list of files from reference folder
		for (String folderName : folderMap.keySet()) {
			List<String> fileList = folderMap.get(folderName);
			// iterate through each file and compare
			for (String fileName : fileList) {
				verifyFile(folderName, fileName);
			}
		}
		for (String fname : deletableFiles) {
			// System.out.print(fname);
		}
		for (String fname : comparedFiles) {
			System.out.println(fname);
		}
		System.out.println(folderMap.size() + " files indexed ...");
		System.out.println(deletableFiles.size() + " files to be deleted ...");
		System.out.println(unmatchedFiles.size() + " modified files found.");

	}// main

	private static Map<String, List> getList(File root) {
		String absoluteFolderName = null;
		List<String> fileList = null;
		if (root.isDirectory()) {
			if (containsFiles(root)) {
				absoluteFolderName = root.getAbsolutePath();
				fileList = new ArrayList<>();
				folderMap.put(absoluteFolderName, fileList);
			}
			File[] fs = root.listFiles();
			for (File f : fs) {
				if (f.isDirectory()) {
					getList(f);
				} else {
					String fileName = f.getName();
					if (isSupportedFormat(fileName.substring(fileName.length() - 3))) {
						fileList.add(f.getName());
					}
				}
			}
		}
		return folderMap;
	}

	private static boolean containsFiles(File root) {
		File[] fs = root.listFiles();
		for (File file : fs) {
			String fileName = file.getName();
			if (file.isFile() && isSupportedFormat(fileName.substring(fileName.length() - 3))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSupportedFormat(String str) {
		return !EXCLUDED_FORMATS_LIST.contains(str);
	}

	private static void verifyFile(String filePath, String fileName2Check) throws IOException {
		File fileRef = new File(filePath + "\\" + fileName2Check);
		filePath = filePath.replace("\\", "/");
		filePath = filePath.replace(REF_FOLDER, FOLDER2CLEAN);
		File file2Check = new File(filePath + "\\" + fileName2Check);
		if (file2Check.exists()) {
			boolean result = false;
			result = comparedFiles(fileRef, file2Check);
			if (result) {
				try {
					deletableFiles.add(file2Check.getName());
					comparedFiles.add(file2Check.getName() + " - same");
					if (DELETE_SAME_FILES) {
						Files.delete(file2Check.toPath());
						File tempfile = new File(filePath + "\\" + fileName2Check + "-meta.xml");
						Files.delete(tempfile.toPath());
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} else {
				unmatchedFiles.add(file2Check.getName());
				comparedFiles.add(file2Check.getName() + " - mismatch");
			}
		}
	}

	private static boolean comparedFiles(File fileRef, File file2Check) throws IOException {
		boolean mismatch = true;
		boolean mismatchFound = true;
		BufferedReader br1 = new BufferedReader(new FileReader(fileRef));
		BufferedReader br2 = new BufferedReader(new FileReader(file2Check));
		String str1 = br1.readLine();
		String str2 = br2.readLine();
		while (str1 != null) {
			if (str2 != null) {
				if (!str1.equals(str2)) {
					mismatchFound = true;
					break;
				}
			}
			str1 = br1.readLine();
			str2 = br2.readLine();
		}
		br1.close();
		br2.close();
		if (str2 == null && !mismatchFound) {
			mismatch = false;
		}
		return !mismatch;
	}

}// class