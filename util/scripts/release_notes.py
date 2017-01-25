#!/usr/bin/python

# Script to generate a basic list of release note issues
# usage : update milestone to one you want to generate release notes for
# run with python release_notes.py
# Your notes will be written to the file temp.md

import json
import time
import urllib2
import argparse

milestone = '2.2.11'
labels = ['New and noteworthy', 'Resolved issues']
repo = 'akvo/akvo-flow-mobile'
api_url = 'https://api.github.com/search/issues?q=milestone:' + milestone + '+repo:' + repo + '+label:'
output_filename = 'temp.md'

def load_issues(url):
    """
    Loads all github issues given a url

    Parameters
    ----------
    url : str
        The full url to use

    Returns
    -------
    json array
        containing issues

    """
    github_request = urllib2.urlopen(url)
    if not github_request:
        parser = argparse.ArgumentParser()
        parser.error('Error getting issues list.')
    decoder = json.JSONDecoder()
    json_result = decoder.decode(github_request.read())
    github_request.close()
    return json_result['items']

def write_issues(f, issues):
    """
    Writes issues to a file

    Parameters
    ----------
    f : file
        The file to use
    issues : json array
        The json array with issues

    Returns
    -------
        nothing
    """
    for issue in issues:
        f.write('* **' + issue['title'] + '** - [#' + str(issue['number']) + '](' + issue[
            'html_url'] + ')\n')

f = open(output_filename, 'w')
f.write('# ver ' + milestone + '\n')
f.write('Date: '+ time.strftime("%d %B %Y"))
f.write('\n')

for label in labels:
    f.write('\n\n# ' + label + '\n')
    write_issues(f, load_issues(api_url + '"' + label.replace(" ", "+") + '"'))

f.close()
