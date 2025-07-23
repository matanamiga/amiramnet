import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'task_model.dart';
import 'package:uuid/uuid.dart';

final taskListProvider = StateNotifierProvider<TaskListNotifier, List<Task>>((ref) {
  return TaskListNotifier();
});

class TaskListNotifier extends StateNotifier<List<Task>> {
  TaskListNotifier() : super([]);
  final _uuid = const Uuid();

  void addTask(String title, String description) {
    final task = Task(id: _uuid.v4(), title: title, description: description);
    state = [...state, task];
  }

  void toggleComplete(String id) {
    state = [
      for (final t in state)
        if (t.id == id) Task(id: t.id, title: t.title, description: t.description, dueDate: t.dueDate, isCompleted: !t.isCompleted) else t
    ];
  }
}
