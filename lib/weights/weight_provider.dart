import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'weight_model.dart';
import 'package:uuid/uuid.dart';

final weightListProvider = StateNotifierProvider<WeightListNotifier, List<WeightEntry>>((ref) {
  return WeightListNotifier();
});

class WeightListNotifier extends StateNotifier<List<WeightEntry>> {
  WeightListNotifier() : super([]);
  final _uuid = const Uuid();

  void addWeight(double value) {
    final entry = WeightEntry(id: _uuid.v4(), value: value, timestamp: DateTime.now());
    state = [...state, entry];
  }
}
