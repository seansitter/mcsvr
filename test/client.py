#!/usr/bin/env python

from pymemcache.client.base import Client
import unittest
import time

class TestGetMany(unittest.TestCase):

    def setUp(self):
        self.client = Client(('localhost', 11211))

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

