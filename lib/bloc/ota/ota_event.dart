part of 'ota_bloc.dart';

@immutable
sealed class OtaEvent {}

class OtaInitialEvent extends OtaEvent {}
class OtaCheckAuthorizationEvent extends OtaEvent {}