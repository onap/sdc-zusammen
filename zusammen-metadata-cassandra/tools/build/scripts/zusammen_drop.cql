CREATE KEYSPACE IF NOT EXISTS zusammen_Test
WITH REPLICATION = {
	'class' : 'SimpleStrategy',
	'replication_factor' : 3
};

use zusammen_Test;

DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS version;
DROP TABLE IF EXISTS version_elements;
DROP TABLE IF EXISTS element_namespace;
DROP TABLE IF EXISTS element;
