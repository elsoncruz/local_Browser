


// ignore_for_file: use_build_context_synchronously

import 'dart:developer';
import 'dart:io';

import 'package:chat_as/api/apis.dart';
import 'package:chat_as/helper/dialogs.dart';
import 'package:chat_as/main.dart';
import 'package:chat_as/screen/home_screen.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => MyWidgetState();
}

class MyWidgetState extends State<LoginScreen> {
    // ignore: unused_field
    bool _isAnimate=false;
  

  @override
  void initState(){
    super.initState();
    Future.delayed(const Duration(microseconds: 500),(){
      setState(() {
        _isAnimate=true;
      });
    });
  }

  _handelGoogleLogin(){
    Dialogs.showProgressBar(context);
    _signInWithGoogle().then((user) async {
      Navigator.pop(context);
      if(user!=null){
        log('\nUser:${user.user}');
        log('\nUserAdditionalInfo:${user.additionalUserInfo}');

        if((await APIs.userExists())){
          Navigator.pushReplacement(
            context, MaterialPageRoute(builder: (_)=>const HomeScreen()));
        }else{
          await APIs.createUser().then((value){
            Navigator.pushReplacement(
              context, MaterialPageRoute(builder: (_)=>const HomeScreen()));
          });
        }   
      }   
    });
  }
  
  Future<UserCredential?> _signInWithGoogle() async {
  try{
      await InternetAddress.lookup('google.com');
  // Trigger the authentication flow
      final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();

  // Obtain the auth details from the request
      final GoogleSignInAuthentication? googleAuth = await googleUser?.authentication;

  // Create a new credential
    final credential = GoogleAuthProvider.credential(
      accessToken: googleAuth?.accessToken,
      idToken: googleAuth?.idToken,
  );

  // Once signed in, return the UserCredential
  return await APIs.auth.signInWithCredential(credential);
}catch(e){
      log('\n_signInWithGoogle:$e');
      Dialogs.showSnackbar(context,'somthing Went Wrong to Internet!');
      return null;
    }
}

  @override
  Widget build(BuildContext context) {
    mq=MediaQuery.of(context).size;
    return  Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('Welcome to Chat Us'),
        ),
        body: Stack(children: [
          AnimatedPositioned(
            top: mq.height*.15,
            right:_isAnimate? mq.width*.25:-mq.width*.5,
            width: mq.width*.5,
            duration: const Duration(seconds: 3),
            child: Image.asset('images/779461.png')),
          Positioned(
            bottom: mq.height*.15,
            left: mq.width*.05,
            width: mq.width*.9,
            height: mq.height*.05,
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color.fromARGB(255, 211, 244, 250),
                elevation: 1),
            onPressed: (){
              _handelGoogleLogin();
            }, 
            
            icon: Image.asset('images/search.png',height: mq.height*.03,), 
            
              label: RichText(text: const TextSpan(
                style: TextStyle(color: Colors.black,fontSize: 15),
                children: [
                TextSpan(text: 'Login with'),
                TextSpan(text: 'Google',style: TextStyle(fontWeight: FontWeight.w500)),
              ]),))),
        ]),
    );
  }
}