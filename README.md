# google_doc_to_wiki
Move Google Docs to Wiki.

This project moves Google Docs to a local MediaWiki site.

Only supports documents and not spreadsheets and presentations.

The project contains a Intellij project file that can be used to import to Intellij Idea.

To run, run /bin/run.sh. You need a local MediaWiki setup to which the Google Docs can be migrated.

An example run:

./run.sh --username [your_googledoc_usrname] --password [your_googledoc_passwd]

This is a demo of the GoogleDoc migration!
Using this interface, you can list and migrate your Google Docs.
Type 'help' for a list of commands.

Command: list documents
 -- Understanding bird migration document:1uWr4U3-i5oZ0wlpS9YG6KkTBuSwgSQFVce88NKIkuLM

Command: migrate 1uWr4U3-i5oZ0wlpS9YG6KkTBuSwgSQFVce88NKIkuLM
The document "Understanding bird migration" is successfully migrated under "Default"

Command: exit