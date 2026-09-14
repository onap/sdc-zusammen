CREATE KEYSPACE IF NOT EXISTS zusammen_test
WITH REPLICATION = {
	'class' : 'SimpleStrategy',
	'replication_factor' : 3
};

use zusammen_test;

CREATE TABLE IF NOT EXISTS item (
	item_id text primary key,
	item_info text,
	creation_time timestamp,
	modification_time timestamp
);

CREATE TABLE IF NOT EXISTS version (
	space text,
	item_id text,
	version_id text,
	base_version_id text,
	info text,
	relations text,
	creation_time timestamp,
	modification_time timestamp,
	PRIMARY KEY (( space, item_id ), version_id)
);

CREATE TABLE IF NOT EXISTS version_elements (
	space text,
	item_id text,
	version_id text,
	element_ids set<text>,
	PRIMARY KEY (( space, item_id, version_id ))
);

CREATE TABLE IF NOT EXISTS element_namespace (
	item_id text,
	element_id text,
	namespace text,
	PRIMARY KEY (( item_id, element_id ))
);

CREATE TABLE IF NOT EXISTS element (
	space text,
	item_id text,
	version_id text,
	element_id text,
	parent_id text,
	namespace text,
	info text,
	relations text,
	sub_element_ids set<text>,
	PRIMARY KEY (( space, item_id, version_id, element_id ))
);
