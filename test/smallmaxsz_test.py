#!/usr/bin/env python

from pymemcache.client.base import Client
import unittest
import time
import subprocess
import os
import signal

class SmallMaxSzServerConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        print "Note: this test run will start and stop the server for each test case, this may take a minute or two...\n"

    def setUp(self):
        self.startServer()
        self.client = Client(('localhost', 11211))

    def tearDown(self):
        self.killServer()

    def startServer(self):
        FNULL = open(os.devnull, 'w')
        path = os.path.dirname(os.path.realpath(__file__))
        proc = subprocess.Popen(
            ["java", "-jar", path+"/../bin/mcsvr.jar", "-maxCacheBytes", "30", "-lruRecoverPct", "40"],
            stdout=FNULL, stderr=subprocess.STDOUT
        )
        self.server_pid = proc.pid
        time.sleep(3)

    def killServer(self):
        time.sleep(1)
        os.kill(int(self.server_pid), signal.SIGTERM)

    def testNoEvict(self):
        self.client.set('first_key', '121212121212', 0, False) # 12 bytes
        self.client.set('second_key', '88888888', 0, False) # 8 bytes
        self.client.set('third_key', '999999999', 0, False) # 9 bytes
        time.sleep(.5) # the lru is asynchronous
        res = self.client.get('first_key')
        self.assertEqual('121212121212', res)
        res = self.client.get('second_key')
        self.assertEqual('88888888', res)
        res = self.client.get('third_key')
        self.assertEqual('999999999', res)

    def testEvict1(self):
        self.client.set('first_key', '121212121212', 0, False) # 12 bytes
        self.client.set('second_key', '88888888', 0, False) # 8 bytes
        self.client.set('third_key', '151515151515151', 0, False) # 15 bytes
        time.sleep(.5) # the lru is asynchronous
        res = self.client.get('first_key')
        self.assertEqual(None, res)
        res = self.client.get('second_key')
        self.assertEqual('88888888', res)
        res = self.client.get('third_key')
        self.assertEqual('151515151515151', res)

    def testEvict2(self):
        self.client.set('first_key', '88888888', 0, False) # 8 bytes
        self.client.set('second_key', '88888888', 0, False) # 8 bytes
        self.client.set('third_key', '151515151515151', 0, False) # 15 bytes
        time.sleep(.5) # the lru is asynchronous
        res = self.client.get('first_key')
        self.assertEqual(None, res)
        res = self.client.get('second_key')
        self.assertEqual(None, res)
        res = self.client.get('third_key')
        self.assertEqual('151515151515151', res)

    def testEvict1Touch1(self):
        self.client.set('first_key', '121212121212', 0, False) # 12 bytes
        self.client.set('second_key', '88888888', 0, False) # 8 bytes
        self.client.set('third_key', '88888888', 0, False) # 8 bytes
        self.client.get('first_key') # pull up to mru, now 2nd and 3rd should be gone
        self.client.set('fourth_key', '151515151515151', 0, False) # 15 bytes
        time.sleep(.5) # the lru is asynchronous
        res = self.client.get('second_key')
        self.assertEqual(None, res)
        res = self.client.get('third_key')
        self.assertEqual(None, res)
        res = self.client.get('first_key')
        self.assertEqual('121212121212', res)
        res = self.client.get('fourth_key')
        self.assertEqual('151515151515151', res)

if __name__ == '__main__':
    unittest.main()

