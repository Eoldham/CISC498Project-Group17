function processFolder(directory) {
	list = getFileList(directory);

	for (i = 0; i < list.length; i++) {
		if (File.isDirectory("" + directory + list[i])) {
			processFolder("" + directory + list[i]);
		}
		if (endsWith(list[i], ".lsm")) {
			open("" + directory + list[i]);
		}
	}
}

input = getDirectory("Choose directory.");
processFolder(input);
run("Run Calcium Signal...")