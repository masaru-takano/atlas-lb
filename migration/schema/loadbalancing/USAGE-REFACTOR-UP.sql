USE `loadbalancing`;

INSERT INTO `state`(`state`, `jobname`) VALUES('STOP', 'THE_ONE_TO_RULE_THEM_ALL');

update `meta` set `meta_value` = 'THENEXTVERSION' where `meta_key`='version';
