-- Existing bundled resources become server-addressable placement items.
-- Official Telegram revision: 3de37c8cfb039452be7398909461da6288babf15
-- Assets/triggers are unchanged. Stage only; migration requires manual release.
INSERT OR IGNORE INTO emoji_items
(id,pack_id,alt_emoji,kind,storage_key,byte_size,sha256,enabled,premium_origin,source,pack_name,created_at)
SELECT column1,column2,column3,column4,column5,column6,column7,column8,column9,column10,column11,column12 FROM (VALUES
('bundled_tg_fire_on','pack_tg_ui','🔥','lottie','res:raw/tg_fire_on',19653,'f1befabb68656558b8e57f4ed834367b07842a84210d855f2c060809453b308c',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_admin','pack_tg_ui','👮','lottie','res:raw/tg_ic_admin',11343,'b3807c1d4501ac07c3ac5283472c2e42a8a5364a55722a14c9226236c05c178e',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_pin','pack_tg_ui','📌','lottie','res:raw/tg_ic_pin',8798,'8d11f43669640fc094e10a8ac9e143f2832380e1fe6844c0c1bb786841105c22',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_ban','pack_tg_ui','🚫','lottie','res:raw/tg_ic_ban',10403,'c90a096a3ba115b6e3ab76b4c64cafc4a53d1852790a41c3bee8513e110499af',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_unban','pack_tg_ui','✅','lottie','res:raw/tg_ic_unban',19612,'2f87552980d2dd5c5164203bea8c981aecc3f5dd51a86ef876ad5885695c2a14',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_filters','pack_tg_ui','🔍','lottie','res:raw/tg_filters',125685,'724a4736ea8d90e95043fbed85f763094a91c59c7ac5d3956c80ad82f65e7df6',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_filter_new','pack_tg_ui','✨','lottie','res:raw/tg_filter_new',78665,'cb8ebee5429df65b8fdb2042ed8bc3143a8196cca323f378ed3b7ce6b505488e',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_chats_archive','pack_tg_ui','📦','lottie','res:raw/tg_chats_archive',4880,'1a8782b95d6effbdef607a89ccb1053df82f27d85f1b2b740ab9aa098c860592',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_folder_in','pack_tg_ui','📂','lottie','res:raw/tg_folder_in',17954,'241d713c601b6a2fa54bb3044fdbd2d244058cb4dfa7f7a1f8d1e4a63c7a5751',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_folder_out','pack_tg_ui','📁','lottie','res:raw/tg_folder_out',14579,'085adbb0e1e55ed9e3ecd5a6f3a286e5a2f6aabe2302419486ac2d9e90cc7485',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_delete','pack_tg_ui','🗑️','lottie','res:raw/tg_ic_delete',16556,'7a3de58b0c0edc9fb59349106ef9d4516c2d20703c4d5d943082aef4c84344e5',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_mute','pack_tg_ui','🔕','lottie','res:raw/tg_ic_mute',14320,'e03d69278f158f771c7f51e94e9f6b06d7bb96afcdbc233d54f159b78a1d2a46',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_unmute','pack_tg_ui','🔔','lottie','res:raw/tg_ic_unmute',11796,'ba19e03c06d6c13d68df3c96ebbb46d7957b47bd03cf32a07253a9e3dac158ce',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_ic_unpin','pack_tg_ui','📍','lottie','res:raw/tg_ic_unpin',10576,'5991e663aafa41e313746e11794d56389fea0cd75d1129fb93ccf782b684f7ef',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_qr_code_logo','pack_tg_ui','📱','lottie','res:raw/tg_qr_code_logo',23904,'2a072f8111209cd543929806110c7523082331b6f01ae5ec31c99db41df57b79',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_report_police','pack_tg_ui','🚨','lottie','res:raw/tg_report_police',314497,'d8195c69a3f50bc2398aafbc76b2a38c227a9bbd274148d163ed60e0ca22804c',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_saved_messages','pack_tg_ui','💾','lottie','res:raw/tg_saved_messages',12113,'486739139125b317efb1d06c6dc946413bf1beba6af6d28fd2609d6c97072479',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_statistic_preload','pack_tg_ui','📊','lottie','res:raw/tg_statistic_preload',202498,'d65923c06b10f3978bb31ce083cfb16f06cb9b8ed25b18e2c0cb78ec20c7d3dd',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_swipe_delete','pack_tg_ui','🧹','lottie','res:raw/tg_swipe_delete',6215,'995c2ef94891834164cc4ef2cd68d44b9ea61423aa24318a08d7c0b5c0122da6',1,0,'telegram-ui','Telegram UI Icons',0),
('bundled_tg_tab_poll','pack_tg_ui','🗳️','lottie','res:raw/tg_tab_poll',7750,'f29b476a00323dbc608346e373909c7708afcb0fd07689de190ac7e635f7bff1',1,0,'telegram-ui','Telegram UI Icons',0)) WHERE EXISTS (SELECT 1 FROM emoji_packs WHERE id='pack_tg_ui');
CREATE INDEX IF NOT EXISTS idx_emoji_items_page ON emoji_items(created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS idx_emoji_items_pack_page ON emoji_items(pack_id,created_at DESC,id DESC);
