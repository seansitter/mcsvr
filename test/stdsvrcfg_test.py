#!/usr/bin/env python

from pymemcache.client.base import Client
import unittest
import time
import subprocess
import os
import signal

class StandardServerConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        print "PLEASE MAKE SURE THE SERVER IS NOT ALREADY RUNNING!"
        print "Note: this test run will start and stop the server for each test case, this may take a minute or two...\n"

    def setUp(self):
        self.startServer()
        self.client = Client(('localhost', 11211))

    def tearDown(self):
        self.killServer()

    def startServer(self):
        FNULL = open(os.devnull, 'w')
        path = os.path.dirname(os.path.realpath(__file__))
        proc = subprocess.Popen(["java", "-jar", path+"/../bin/mcsvr.jar"],
                                stdout=FNULL, stderr=subprocess.STDOUT)
        self.server_pid = proc.pid
        time.sleep(3)

    def killServer(self):
        time.sleep(1)
        os.kill(int(self.server_pid), signal.SIGTERM)

    def testGetSingle(self):
        self.client.set('first_key', 'first_value', 0, False)
        res = self.client.get('first_key')
        self.assertEqual(res, "first_value")

    def testGetMany(self):
        self.client.set('first_key', 'first_value', 0, False)
        self.client.set('second_key', 'second_value', 0, True)
        res = self.client.get_many(['first_key', 'second_key'])
        self.assertEqual(res["first_key"], "first_value")
        self.assertEqual(res["second_key"], "second_value")

    def testGetExpired(self):
        self.client.set('first_key', 'first_value', 0, False)
        self.client.set('first_key', 'first_value', 2, False)
        self.client.get_many(['first_key', 'second_key'])
        time.sleep(3)
        self.assertEqual(None, self.client.get('first_key'))

    def testGetDeleted(self):
        self.client.set('first_key', 'first_value', 0, False)
        res = self.client.get('first_key')
        self.assertEqual('first_value', res)
        self.client.delete('first_key')
        res = self.client.get('first_key')
        self.assertEqual(None, res)

    def testUpdateExpired(self):
        self.client.set('first_key', 'first_value', 2, False)
        time.sleep(3)
        self.client.set('first_key', 'second_value', 0, False)
        res = self.client.get('first_key')
        self.assertEqual('second_value', res)

    def testCaseUpdateOk(self):
        self.client.set('first_key', 'first_value', 0, False)
        res = self.client.gets('first_key')
        res = self.client.cas('first_key', 'second_value', res[1], 0, False)
        self.assertTrue(res)
        res = self.client.get('first_key')
        self.assertEqual('second_value', res)

    def testCaseUpdateInvalidUniq(self):
        self.client.set('first_key', 'first_value', 0, False)
        res = self.client.gets('first_key')
        casUniq = res[1]
        self.client.set('first_key', 'second_value', 0, False)
        res = self.client.cas('first_key', 'third_value', casUniq, 0, False)
        self.assertFalse(res)
        res = self.client.get('first_key')
        self.assertEqual('second_value', res)

    def testCaseUpdateNotOkOnExpired(self):
        self.client.set('first_key', 'first_value', 2, False)
        res = self.client.gets('first_key')
        time.sleep(3)
        res = self.client.cas('first_key', 'second_value', res[1], 0, False)
        self.assertEqual(None, res)

    def testCaseUpdateNotOkOnMissing(self):
        self.client.set('first_key', 'first_value', 0, False)
        res = self.client.gets('first_key')
        casUniq = res[1]
        self.client.delete('first_key')
        res = self.client.cas('first_key', 'second_value', casUniq, 0, False)
        self.assertEqual(None, res)

if __name__ == '__main__':
    unittest.main()

