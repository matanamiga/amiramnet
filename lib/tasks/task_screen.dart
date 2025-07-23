import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'task_provider.dart';

class TaskScreen extends ConsumerWidget {
  const TaskScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tasks = ref.watch(taskListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Tasks')),
      body: ListView.builder(
        itemCount: tasks.length,
        itemBuilder: (context, index) {
          final t = tasks[index];
          return CheckboxListTile(
            title: Text(t.title),
            subtitle: Text(t.description),
            value: t.isCompleted,
            onChanged: (_) {
              ref.read(taskListProvider.notifier).toggleComplete(t.id);
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final titleController = TextEditingController();
          final descController = TextEditingController();
          final ok = await showDialog<bool>(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Add Task'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: titleController,
                    decoration: const InputDecoration(hintText: 'Title'),
                  ),
                  TextField(
                    controller: descController,
                    decoration: const InputDecoration(hintText: 'Description'),
                  ),
                ],
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
                TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Add')),
              ],
            ),
          );
          if (ok == true) {
            ref.read(taskListProvider.notifier).addTask(titleController.text, descController.text);
          }
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
