#instructions to run Funf Analyze script

# Funf Analyze Script Tutorial #
The data visualization scripts are provided to give you a sample snapshot of a limited amount of your data and is recommended for data periods of less than a month. The data, itself, is converted to a convenient single SQLite file, allowing you to explore your data separately.

## Steps ##
In order to run the script, you will first need to install python (http://www.python.org/download/) and PIL (http://www.pythonware.com/products/pil/ or `pip install pil` if you have pip set up).

Next, download the [Funf Analyze scripts](https://code.google.com/p/funf-open-sensing-framework/source/browse/?repo=samples&name=v0.4.x), place the exported zip file with the data in the same folder. If you are using Funf in a Box you will need to create this zip file from the raw data in your Dropbox.

Run the funf\_analyze.py script from the terminal and type the password you used to encrypt your data when requested. For Funf in a Box this password will be listed in config/encryption\_password.txt. It is possible to use multiple passwords across the same data. The script will prompt you to input additional passwords if it is unable to decrypt all. Hitting enter with no password will skip any remaining encrypted files. Once done, open data\_visualization.html to view a snapshot of your data.

For exploring all data from all your enabled probes: http://sourceforge.net/projects/sqlitebrowser