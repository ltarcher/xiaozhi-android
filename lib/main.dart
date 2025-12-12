import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/bloc/ota/ota_bloc.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/page/chat_page.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';
import 'package:xiaozhi/util/storage_util.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      systemStatusBarContrastEnforced: true,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarDividerColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.dark,
      statusBarIconBrightness: Brightness.light,
    ),
  );

  SystemChrome.setEnabledSystemUIMode(
    SystemUiMode.edgeToEdge,
    overlays: [SystemUiOverlay.top],
  );

  await SharedPreferencesUtil().init();
  await StorageUtil().init();

  runApp(
    MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (context) => OtaBloc()..add(OtaInitialEvent()),
          lazy: false,
        ),
        BlocProvider(create: (context) => ChatBloc()..add(ChatInitialEvent())),
      ],
      child: MaterialApp(
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Color(0xFF002FA7)),
          useMaterial3: true,
        ),
        localizationsDelegates: [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: [Locale('en'), Locale('zh')],
        home: const MainHomePage(),
      ),
    ),
  );
}

class MainHomePage extends StatefulWidget {
  const MainHomePage({super.key});

  @override
  State<MainHomePage> createState() => _MainHomePageState();
}

class _MainHomePageState extends State<MainHomePage> {
  final List<Widget> _pages = [
    const ChatPage(),
  ];

  @override
  Widget build(BuildContext context) {
    // 只有一个页面，直接返回该页面，不使用BottomNavigationBar
    return Scaffold(
      body: _pages[0],
    );
  }
}