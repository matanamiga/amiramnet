import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'weight_provider.dart';

class WeightScreen extends ConsumerWidget {
  const WeightScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final weights = ref.watch(weightListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Weights')),
      body: ListView.builder(
        itemCount: weights.length,
        itemBuilder: (context, index) {
          final w = weights[index];
          return ListTile(
            title: Text('${w.value} kg'),
            subtitle: Text(w.timestamp.toLocal().toString()),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final controller = TextEditingController();
          final result = await showDialog<double>(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Add Weight'),
              content: TextField(
                controller: controller,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(hintText: 'Enter weight'),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Cancel'),
                ),
                TextButton(
                  onPressed: () {
                    final value = double.tryParse(controller.text);
                    Navigator.pop(context, value);
                  },
                  child: const Text('Add'),
                ),
              ],
            ),
          );
          if (result != null) {
            ref.read(weightListProvider.notifier).addWeight(result);
          }
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
