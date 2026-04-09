// Auto-generated protocol module declarations.
// Each module contains a decoder struct for the corresponding GPS tracking protocol.

pub mod adm;
pub mod ais;
pub mod alematics;
pub mod anytrek;
pub mod apel;
pub mod aplicom;
pub mod appello;
pub mod aquila;
pub mod ardi01;
pub mod arknav;
pub mod arknavx8;
pub mod armoli;
pub mod arnavi;
pub mod astra;
pub mod at2000;
pub mod atrack;
pub mod auro;
pub mod austinnb;
pub mod autofon;
pub mod autograde;
pub mod autotrack;
pub mod avema;
pub mod avl301;
pub mod b2316;
pub mod bce;
pub mod blackkite;
pub mod blue;
pub mod box_;
pub mod bstpl;
pub mod bws;
pub mod c2stek;
pub mod calamp;
pub mod carcell;
pub mod carscop;
pub mod cartrack;
pub mod castel;
pub mod cautela;
pub mod cellocator;
pub mod cguard;
pub mod cityeasy;
pub mod continental;
pub mod cradlepoint;
pub mod dingtek;
pub mod disha;
pub mod dmt;
pub mod dmthttp;
pub mod dolphin;
pub mod dragino;
pub mod dsf22;
pub mod dualcam;
pub mod dway;
pub mod easytrack;
pub mod eelink;
pub mod egts;
pub mod enfora;
pub mod ennfu;
pub mod envotech;
pub mod eseal;
pub mod esky;
pub mod extremtrac;
pub mod fifotrack;
pub mod fleetguide;
pub mod flespi;
pub mod flexapi;
pub mod flexcomm;
pub mod flexiblereport;
pub mod flextrack;
pub mod fox;
pub mod freedom;
pub mod freematics;
pub mod futureway;
pub mod g1rus;
pub mod galileo;
pub mod gator;
pub mod genx;
pub mod gl100;
pub mod gl200;
pub mod gl601;
pub mod globalsat;
pub mod globalstar;
pub mod gnx;
pub mod gosafe;
pub mod gotop;
pub mod gps056;
pub mod gps103;
pub mod gpsgate;
pub mod gpsmarker;
pub mod gpsmta;
pub mod granit;
pub mod gs100;
pub mod gt02;
pub mod gt06;
pub mod gt30;
pub mod h02;
pub mod haicom;
pub mod homtecs;
pub mod hoopo;
pub mod huabao;
pub mod huasheng;
pub mod hunterpro;
pub mod hyn600;
pub mod idpl;
pub mod intellitrac;
pub mod iotm;
pub mod its;
pub mod ivt401;
pub mod jido;
pub mod jmak;
pub mod jpkorjar;
pub mod jt600;
pub mod kenji;
pub mod khd;
pub mod l100;
pub mod laipac;
pub mod leafspy;
pub mod m2c;
pub mod m2m;
pub mod maestro;
pub mod manpower;
pub mod mavlink2;
pub mod megastek;
pub mod meiligao;
pub mod meitrack;
pub mod mictrack;
pub mod milesmate;
pub mod minifinder2;
pub mod minifinder;
pub mod mobilogix;
pub mod moovbox;
pub mod motor;
pub mod mta6;
pub mod mtx;
pub mod mxt;
pub mod navigil;
pub mod navis;
pub mod naviset;
pub mod navtelecom;
pub mod ndtpv6;
pub mod neos;
pub mod net;
pub mod niot;
pub mod noran;
pub mod nto;
pub mod nvs;
pub mod nyitech;
pub mod obddongle;
pub mod oigo;
pub mod oko;
pub mod omnicomm;
pub mod opengts;
pub mod orbcomm;
pub mod orion;
pub mod osmand;
pub mod outsafe;
pub mod owntracks;
pub mod pacifictrack;
pub mod pathway;
pub mod piligrim;
pub mod plugin;
pub mod polte;
pub mod portman;
pub mod positrex;
pub mod pretrace;
pub mod pricol;
pub mod progress;
pub mod pst;
pub mod pt215;
pub mod pt3000;
pub mod pt502;
pub mod pt60;
pub mod pui;
pub mod r12w;
pub mod racedynamics;
pub mod radar;
pub mod ramac;
pub mod raveon;
pub mod recoda;
pub mod retranslator;
pub mod rftrack;
pub mod riti;
pub mod robotrack;
pub mod rst;
pub mod ruptela;
pub mod s168;
pub mod sabertek;
pub mod sanav;
pub mod sanul;
pub mod satsol;
pub mod sigfox;
pub mod siwi;
pub mod skypatrol;
pub mod smartcar;
pub mod smartsole;
pub mod smokey;
pub mod snapper;
pub mod solarpowered;
pub mod spot;
pub mod starcom;
pub mod starlink;
pub mod startek;
pub mod stb;
pub mod stl060;
pub mod suntech;
pub mod supermate;
pub mod svias;
pub mod swiftech;
pub mod t55;
pub mod t57;
pub mod t622iridium;
pub mod t800x;
pub mod taip;
pub mod techtlt;
pub mod techtocruz;
pub mod tek;
pub mod telemax;
pub mod telic;
pub mod teltonika;
pub mod teratrack;
pub mod thinkpower;
pub mod thinkrace;
pub mod thuraya;
pub mod tk102;
pub mod tk103;
pub mod tlt2h;
pub mod tlv;
pub mod tmg;
pub mod topflytech;
pub mod topin;
pub mod totem;
pub mod tr20;
pub mod tr900;
pub mod trackbox;
pub mod trakmate;
pub mod tramigo;
pub mod transync;
pub mod trv;
pub mod tt8850;
pub mod ttnhttp;
pub mod tytan;
pub mod tzone;
pub mod ulbotech;
pub mod upro;
pub mod uux;
pub mod v680;
pub mod valtrack;
pub mod visiontek;
pub mod vlt;
pub mod vnet;
pub mod vt200;
pub mod vtfms;
pub mod watch;
pub mod wialon;
pub mod wli;
pub mod wondex;
pub mod wristband;
pub mod xexun2;
pub mod xexun;
pub mod xirgo;
pub mod xrb28;
pub mod xt013;
pub mod xt2400;
pub mod ywt;

use crate::ProtocolRegistry;

/// Register all protocol decoders with the protocol registry.
pub fn register_all(_registry: &mut ProtocolRegistry) {
    // Protocol decoders are registered here as they are fully implemented.
    // Each protocol module contains a decoder struct with a placeholder implementation.
    //
    // Example registration (once ProtocolDefinition factories are wired up):
    //   registry.register(ProtocolDefinition { name: "gt06".into(), ... });
    //
    // Currently available protocol modules:
    //   - adm (adm::AdmDecoder)
    //   - ais (ais::AisDecoder)
    //   - alematics (alematics::AlematicsDecoder)
    //   - anytrek (anytrek::AnytrekDecoder)
    //   - apel (apel::ApelDecoder)
    //   - aplicom (aplicom::AplicomDecoder)
    //   - appello (appello::AppelloDecoder)
    //   - aquila (aquila::AquilaDecoder)
    //   - ardi01 (ardi01::Ardi01Decoder)
    //   - arknav (arknav::ArknavDecoder)
    //   - arknavx8 (arknavx8::ArknavX8Decoder)
    //   - armoli (armoli::ArmoliDecoder)
    //   - arnavi (arnavi::ArnaviDecoder)
    //   - astra (astra::AstraDecoder)
    //   - at2000 (at2000::At2000Decoder)
    //   - atrack (atrack::AtrackDecoder)
    //   - auro (auro::AuroDecoder)
    //   - austinnb (austinnb::AustinnbDecoder)
    //   - autofon (autofon::AutofonDecoder)
    //   - autograde (autograde::AutogradeDecoder)
    //   - autotrack (autotrack::AutotrackDecoder)
    //   - avema (avema::AvemaDecoder)
    //   - avl301 (avl301::Avl301Decoder)
    //   - b2316 (b2316::B2316Decoder)
    //   - bce (bce::BceDecoder)
    //   - blackkite (blackkite::BlackkiteDecoder)
    //   - blue (blue::BlueDecoder)
    //   - box (box_::BoxDecoder)
    //   - bstpl (bstpl::BstplDecoder)
    //   - bws (bws::BwsDecoder)
    //   - c2stek (c2stek::C2stekDecoder)
    //   - calamp (calamp::CalampDecoder)
    //   - carcell (carcell::CarcellDecoder)
    //   - carscop (carscop::CarscopDecoder)
    //   - cartrack (cartrack::CartrackDecoder)
    //   - castel (castel::CastelDecoder)
    //   - cautela (cautela::CautelaDecoder)
    //   - cellocator (cellocator::CellocatorDecoder)
    //   - cguard (cguard::CguardDecoder)
    //   - cityeasy (cityeasy::CityeasyDecoder)
    //   - continental (continental::ContinentalDecoder)
    //   - cradlepoint (cradlepoint::CradlepointDecoder)
    //   - dingtek (dingtek::DingtekDecoder)
    //   - disha (disha::DishaDecoder)
    //   - dmt (dmt::DmtDecoder)
    //   - dmthttp (dmthttp::DmtHttpDecoder)
    //   - dolphin (dolphin::DolphinDecoder)
    //   - dragino (dragino::DraginoDecoder)
    //   - dsf22 (dsf22::Dsf22Decoder)
    //   - dualcam (dualcam::DualcamDecoder)
    //   - dway (dway::DwayDecoder)
    //   - easytrack (easytrack::EasytrackDecoder)
    //   - eelink (eelink::EelinkDecoder)
    //   - egts (egts::EgtsDecoder)
    //   - enfora (enfora::EnforaDecoder)
    //   - ennfu (ennfu::EnnfuDecoder)
    //   - envotech (envotech::EnvotechDecoder)
    //   - eseal (eseal::EsealDecoder)
    //   - esky (esky::EskyDecoder)
    //   - extremtrac (extremtrac::ExtremtracDecoder)
    //   - fifotrack (fifotrack::FifotrackDecoder)
    //   - fleetguide (fleetguide::FleetguideDecoder)
    //   - flespi (flespi::FlespiDecoder)
    //   - flexapi (flexapi::FlexapiDecoder)
    //   - flexcomm (flexcomm::FlexcommDecoder)
    //   - flexiblereport (flexiblereport::FlexiblereportDecoder)
    //   - flextrack (flextrack::FlextrackDecoder)
    //   - fox (fox::FoxDecoder)
    //   - freedom (freedom::FreedomDecoder)
    //   - freematics (freematics::FreematicsDecoder)
    //   - futureway (futureway::FuturewayDecoder)
    //   - g1rus (g1rus::G1rusDecoder)
    //   - galileo (galileo::GalileoDecoder)
    //   - gator (gator::GatorDecoder)
    //   - genx (genx::GenxDecoder)
    //   - gl100 (gl100::Gl100Decoder)
    //   - gl200 (gl200::Gl200Decoder)
    //   - gl601 (gl601::Gl601Decoder)
    //   - globalsat (globalsat::GlobalsatDecoder)
    //   - globalstar (globalstar::GlobalstarDecoder)
    //   - gnx (gnx::GnxDecoder)
    //   - gosafe (gosafe::GosafeDecoder)
    //   - gotop (gotop::GotopDecoder)
    //   - gps056 (gps056::Gps056Decoder)
    //   - gps103 (gps103::Gps103Decoder)
    //   - gpsgate (gpsgate::GpsgateDecoder)
    //   - gpsmarker (gpsmarker::GpsmarkerDecoder)
    //   - gpsmta (gpsmta::GpsmtaDecoder)
    //   - granit (granit::GranitDecoder)
    //   - gs100 (gs100::Gs100Decoder)
    //   - gt02 (gt02::Gt02Decoder)
    //   - gt06 (gt06::Gt06Decoder)
    //   - gt30 (gt30::Gt30Decoder)
    //   - h02 (h02::H02Decoder)
    //   - haicom (haicom::HaicomDecoder)
    //   - homtecs (homtecs::HomtecsDecoder)
    //   - hoopo (hoopo::HoopoDecoder)
    //   - huabao (huabao::HuabaoDecoder)
    //   - huasheng (huasheng::HuashengDecoder)
    //   - hunterpro (hunterpro::HunterproDecoder)
    //   - hyn600 (hyn600::Hyn600Decoder)
    //   - idpl (idpl::IdplDecoder)
    //   - intellitrac (intellitrac::IntellitracDecoder)
    //   - iotm (iotm::IotmDecoder)
    //   - its (its::ItsDecoder)
    //   - ivt401 (ivt401::Ivt401Decoder)
    //   - jido (jido::JidoDecoder)
    //   - jmak (jmak::JmakDecoder)
    //   - jpkorjar (jpkorjar::JpkorjarDecoder)
    //   - jt600 (jt600::Jt600Decoder)
    //   - kenji (kenji::KenjiDecoder)
    //   - khd (khd::KhdDecoder)
    //   - l100 (l100::L100Decoder)
    //   - laipac (laipac::LaipacDecoder)
    //   - leafspy (leafspy::LeafspyDecoder)
    //   - m2c (m2c::M2cDecoder)
    //   - m2m (m2m::M2mDecoder)
    //   - maestro (maestro::MaestroDecoder)
    //   - manpower (manpower::ManpowerDecoder)
    //   - mavlink2 (mavlink2::Mavlink2Decoder)
    //   - megastek (megastek::MegastekDecoder)
    //   - meiligao (meiligao::MeiligaoDecoder)
    //   - meitrack (meitrack::MeitrackDecoder)
    //   - mictrack (mictrack::MictrackDecoder)
    //   - milesmate (milesmate::MilesmateDecoder)
    //   - minifinder (minifinder::MinifinderDecoder)
    //   - minifinder2 (minifinder2::Minifinder2Decoder)
    //   - mobilogix (mobilogix::MobilogixDecoder)
    //   - moovbox (moovbox::MoovboxDecoder)
    //   - motor (motor::MotorDecoder)
    //   - mta6 (mta6::Mta6Decoder)
    //   - mtx (mtx::MtxDecoder)
    //   - mxt (mxt::MxtDecoder)
    //   - navigil (navigil::NavigilDecoder)
    //   - navis (navis::NavisDecoder)
    //   - naviset (naviset::NavisetDecoder)
    //   - navtelecom (navtelecom::NavtelecomDecoder)
    //   - ndtpv6 (ndtpv6::Ndtpv6Decoder)
    //   - neos (neos::NeosDecoder)
    //   - net (net::NetDecoder)
    //   - niot (niot::NiotDecoder)
    //   - noran (noran::NoranDecoder)
    //   - nto (nto::NtoDecoder)
    //   - nvs (nvs::NvsDecoder)
    //   - nyitech (nyitech::NyitechDecoder)
    //   - obddongle (obddongle::ObddongleDecoder)
    //   - oigo (oigo::OigoDecoder)
    //   - oko (oko::OkoDecoder)
    //   - omnicomm (omnicomm::OmnicommDecoder)
    //   - opengts (opengts::OpengtsDecoder)
    //   - orbcomm (orbcomm::OrbcommDecoder)
    //   - orion (orion::OrionDecoder)
    //   - osmand (osmand::OsmandDecoder)
    //   - outsafe (outsafe::OutsafeDecoder)
    //   - owntracks (owntracks::OwntracksDecoder)
    //   - pacifictrack (pacifictrack::PacifictrackDecoder)
    //   - pathway (pathway::PathwayDecoder)
    //   - piligrim (piligrim::PiligrimDecoder)
    //   - plugin (plugin::PluginDecoder)
    //   - polte (polte::PolteDecoder)
    //   - portman (portman::PortmanDecoder)
    //   - positrex (positrex::PositrexDecoder)
    //   - pretrace (pretrace::PretraceDecoder)
    //   - pricol (pricol::PricolDecoder)
    //   - progress (progress::ProgressDecoder)
    //   - pst (pst::PstDecoder)
    //   - pt215 (pt215::Pt215Decoder)
    //   - pt3000 (pt3000::Pt3000Decoder)
    //   - pt502 (pt502::Pt502Decoder)
    //   - pt60 (pt60::Pt60Decoder)
    //   - pui (pui::PuiDecoder)
    //   - r12w (r12w::R12wDecoder)
    //   - racedynamics (racedynamics::RacedynamicsDecoder)
    //   - radar (radar::RadarDecoder)
    //   - ramac (ramac::RamacDecoder)
    //   - raveon (raveon::RaveonDecoder)
    //   - recoda (recoda::RecodaDecoder)
    //   - retranslator (retranslator::RetranslatorDecoder)
    //   - rftrack (rftrack::RftrackDecoder)
    //   - riti (riti::RitiDecoder)
    //   - robotrack (robotrack::RobotrackDecoder)
    //   - rst (rst::RstDecoder)
    //   - ruptela (ruptela::RuptelaDecoder)
    //   - s168 (s168::S168Decoder)
    //   - sabertek (sabertek::SabertekDecoder)
    //   - sanav (sanav::SanavDecoder)
    //   - sanul (sanul::SanulDecoder)
    //   - satsol (satsol::SatsolDecoder)
    //   - sigfox (sigfox::SigfoxDecoder)
    //   - siwi (siwi::SiwiDecoder)
    //   - skypatrol (skypatrol::SkypatrolDecoder)
    //   - smartcar (smartcar::SmartcarDecoder)
    //   - smartsole (smartsole::SmartsoleDecoder)
    //   - smokey (smokey::SmokeyDecoder)
    //   - snapper (snapper::SnapperDecoder)
    //   - solarpowered (solarpowered::SolarpoweredDecoder)
    //   - spot (spot::SpotDecoder)
    //   - starcom (starcom::StarcomDecoder)
    //   - starlink (starlink::StarlinkDecoder)
    //   - startek (startek::StartekDecoder)
    //   - stb (stb::StbDecoder)
    //   - stl060 (stl060::Stl060Decoder)
    //   - suntech (suntech::SuntechDecoder)
    //   - supermate (supermate::SupermateDecoder)
    //   - svias (svias::SviasDecoder)
    //   - swiftech (swiftech::SwiftechDecoder)
    //   - t55 (t55::T55Decoder)
    //   - t57 (t57::T57Decoder)
    //   - t622iridium (t622iridium::T622IridiumDecoder)
    //   - t800x (t800x::T800xDecoder)
    //   - taip (taip::TaipDecoder)
    //   - techtlt (techtlt::TechtltDecoder)
    //   - techtocruz (techtocruz::TechtocruzDecoder)
    //   - tek (tek::TekDecoder)
    //   - telemax (telemax::TelemaxDecoder)
    //   - telic (telic::TelicDecoder)
    //   - teltonika (teltonika::TeltonikaDecoder)
    //   - teratrack (teratrack::TeratrackDecoder)
    //   - thinkpower (thinkpower::ThinkpowerDecoder)
    //   - thinkrace (thinkrace::ThinkraceDecoder)
    //   - thuraya (thuraya::ThurayaDecoder)
    //   - tk102 (tk102::Tk102Decoder)
    //   - tk103 (tk103::Tk103Decoder)
    //   - tlt2h (tlt2h::Tlt2hDecoder)
    //   - tlv (tlv::TlvDecoder)
    //   - tmg (tmg::TmgDecoder)
    //   - topflytech (topflytech::TopflytechDecoder)
    //   - topin (topin::TopinDecoder)
    //   - totem (totem::TotemDecoder)
    //   - tr20 (tr20::Tr20Decoder)
    //   - tr900 (tr900::Tr900Decoder)
    //   - trackbox (trackbox::TrackboxDecoder)
    //   - trakmate (trakmate::TrakmateDecoder)
    //   - tramigo (tramigo::TramigoDecoder)
    //   - transync (transync::TransyncDecoder)
    //   - trv (trv::TrvDecoder)
    //   - tt8850 (tt8850::Tt8850Decoder)
    //   - ttnhttp (ttnhttp::TtnhttpDecoder)
    //   - tytan (tytan::TytanDecoder)
    //   - tzone (tzone::TzoneDecoder)
    //   - ulbotech (ulbotech::UlbotechDecoder)
    //   - upro (upro::UproDecoder)
    //   - uux (uux::UuxDecoder)
    //   - v680 (v680::V680Decoder)
    //   - valtrack (valtrack::ValtrackDecoder)
    //   - visiontek (visiontek::VisiontekDecoder)
    //   - vlt (vlt::VltDecoder)
    //   - vnet (vnet::VnetDecoder)
    //   - vt200 (vt200::Vt200Decoder)
    //   - vtfms (vtfms::VtfmsDecoder)
    //   - watch (watch::WatchDecoder)
    //   - wialon (wialon::WialonDecoder)
    //   - wli (wli::WliDecoder)
    //   - wondex (wondex::WondexDecoder)
    //   - wristband (wristband::WristbandDecoder)
    //   - xexun (xexun::XexunDecoder)
    //   - xexun2 (xexun2::Xexun2Decoder)
    //   - xirgo (xirgo::XirgoDecoder)
    //   - xrb28 (xrb28::Xrb28Decoder)
    //   - xt013 (xt013::Xt013Decoder)
    //   - xt2400 (xt2400::Xt2400Decoder)
    //   - ywt (ywt::YwtDecoder)
    // Register fully implemented protocols
    gps103::register(registry);
    gt06::register(registry);
    h02::register(registry);
    osmand::register(registry);
    teltonika::register(registry);

    tracing::info!("Registered 5 protocol implementations, {} stub modules loaded", 258);
}
