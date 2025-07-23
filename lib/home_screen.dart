import 'package:flutter/material.dart';
import 'weights/weight_screen.dart';
import 'tasks/task_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _index = 0;
  final _screens = const [WeightScreen(), TaskScreen()];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_index],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _index,
        onTap: (i) => setState(() => _index = i),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.fitness_center), label: 'Weight'),
          BottomNavigationBarItem(icon: Icon(Icons.check_box), label: 'Tasks'),
        ],
      ),
    );
  }
}
